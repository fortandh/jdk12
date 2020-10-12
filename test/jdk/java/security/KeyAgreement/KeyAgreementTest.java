/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

 /*
 * @test
 * @bug 4936763 8184359 8205476
 * @summary KeyAgreement Test with all supported algorithms from JCE.
 *          Arguments order <KeyExchangeAlgorithm> <KeyGenAlgorithm> <Provider>
 *          It removes com/sun/crypto/provider/KeyAgreement/DHGenSecretKey.java
 *          as the same functionality for DiffieHellman is covered along with
 *          this test file was covered before with JDK-4936763.
 * @run main/othervm -Djdk.crypto.KeyAgreement.legacyKDF=true KeyAgreementTest
 *          DiffieHellman DH SunJCE
 * @run main KeyAgreementTest ECDH EC SunEC
 * @run main KeyAgreementTest XDH XDH SunEC
 */
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.NamedParameterSpec;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHGenParameterSpec;

public class KeyAgreementTest {

    public static void main(String[] args) throws Exception {

        String kaAlgo = args[0];
        String kpgAlgo = args[1];
        String provider = args[2];
        AlgoSpec aSpec = AlgoSpec.valueOf(AlgoSpec.class, kaAlgo);
        List<AlgorithmParameterSpec> specs = aSpec.getAlgorithmParameterSpecs();
        for (AlgorithmParameterSpec spec : specs) {
            testKeyAgreement(provider, kaAlgo, kpgAlgo, spec);
        }
    }

    /**
     * Generate AlgorithmParameterSpec using all possible supported curve for
     * KeyExchangeAlgorithm.
     */
    private enum AlgoSpec {
        // EC curve supported for KeyGeneration can found between intersection
        // of curves define in
        // "java.base/share/classes/sun/security/util/CurveDB.java"
        // and
        // "jdk.crypto.ec/share/native/libsunec/impl/ecdecode.c"
        ECDH(
                // SEC2 prime curves
                "secp112r1", "secp112r2", "secp128r1", "secp128r2", "secp160k1",
                "secp160r1", "secp192k1", "secp192r1", "secp224k1", "secp224r1",
                "secp256k1", "secp256r1", "secp384r1", "secp521r1",
                // ANSI X9.62 prime curves
                "X9.62 prime192v2", "X9.62 prime192v3", "X9.62 prime239v1",
                "X9.62 prime239v2", "X9.62 prime239v3",
                // SEC2 binary curves
                "sect113r1", "sect113r2", "sect131r1", "sect131r2", "sect163k1",
                "sect163r1", "sect163r2", "sect193r1", "sect193r2", "sect233k1",
                "sect233r1", "sect239k1", "sect283k1", "sect283r1", "sect409k1",
                "sect409r1", "sect571k1", "sect571r1",
                // ANSI X9.62 binary curves
                "X9.62 c2tnb191v1", "X9.62 c2tnb191v2", "X9.62 c2tnb191v3",
                "X9.62 c2tnb239v1", "X9.62 c2tnb239v2", "X9.62 c2tnb239v3",
                "X9.62 c2tnb359v1", "X9.62 c2tnb431r1"
        ),
        XDH("X25519", "X448"),
        // There is no curve for DiffieHellman
        DiffieHellman(new String[]{});

        private final List<AlgorithmParameterSpec> specs = new ArrayList<>();

        private AlgoSpec(String... curves) {
            // Generate AlgorithmParameterSpec for each KeyExchangeAlgorithm
            for (String crv : curves) {
                switch (this.name()) {
                    case "ECDH":
                        specs.add(new ECGenParameterSpec(crv));
                        break;
                    case "XDH":
                        specs.add(new NamedParameterSpec(crv));
                        break;
                    case "DiffieHellman":
                        specs.add(new DHGenParameterSpec(512, 64));
                        break;
                    default:
                        throw new RuntimeException("Invalid Algo name "
                                + this.name());
                }
            }
        }

        public List<AlgorithmParameterSpec> getAlgorithmParameterSpecs() {
            return this.specs;
        }
    }

    /**
     * Perform KeyAgreement operation using native as well as JCE provider.
     */
    private static void testKeyAgreement(String provider, String kaAlgo,
            String kpgAlgo, AlgorithmParameterSpec spec) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance(kpgAlgo, provider);
        kpg.initialize(spec);
        KeyPair kp1 = kpg.generateKeyPair();
        KeyPair kp2 = kpg.generateKeyPair();

        // Uses KeyAgreement based on Provider search order.
        KeyAgreement ka1 = KeyAgreement.getInstance(kaAlgo);
        ka1.init(kp1.getPrivate());
        ka1.doPhase(kp2.getPublic(), true);
        byte[] secret1 = ka1.generateSecret();

        // Uses SunJCE provider
        KeyAgreement ka2 = KeyAgreement.getInstance(kaAlgo, provider);
        ka2.init(kp2.getPrivate());
        ka2.doPhase(kp1.getPublic(), true);
        // Keeping the legacy generateSecret method for DiffieHellman as it was
        // defined in removed Test file from JDK-4936763,
        // com/sun/crypto/provider/KeyAgreement/DHGenSecretKey.java.
        byte[] secret2 = "DiffieHellman".equals(kaAlgo)
                ? ka2.generateSecret("AES").getEncoded() : ka2.generateSecret();

        // With related keypairs, each provider should generate same
        // KeyAgreement secret.
        if (!Arrays.equals(secret1, secret2)) {
            throw new Exception("KeyAgreement secret mismatch.");
        }

        // ensure that a new secret cannot be produced before the next doPhase
        try {
            ka2.generateSecret();
            throw new RuntimeException("state not reset");
        } catch (IllegalStateException ex) {
            // this is expected
        }

        // calling doPhase and then generateSecret should succeed
        ka2.doPhase(kp1.getPublic(), true);
        ka2.generateSecret();
    }
}