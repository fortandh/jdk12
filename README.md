# No error, no warning jdk12 for linux!
## For author
When I downloaded the jdk12 from the [hg.openjdk.java.net](https://hg.openjdk.java.net/jdk/jdk12/file/06222165c35f), I compiled it with gcc 9.3.0, and found that there are many warnings in it, and the policy that gcc used force me to solve the warnings, or just ignore them.

If I just ignore them, I could compiled them quickly. But I don't do so, and I know, it's a chanllenge for me.

Now, I have finished it. I know, others would think it's a small step for them, but for me, it's a big step.

## Q&A
### Requirements
* System: Ubuntu 18.04 LTS
* Make: GNU Make 4.1
* Gcc: 7.5.0
* G++: 7.5.0

### Stop at vm_version_x86.cpp:565 while debugging
![get_cpu_info_stub_problem.png](img%2Fget_cpu_info_stub_problem.png)

**Using continue in gdb to pass this segment fault.**
![get_cpu_info_stub_solution.png](img%2Fget_cpu_info_stub_solution.png)

Reference: https://bugzilla.redhat.com/show_bug.cgi?id=1572811