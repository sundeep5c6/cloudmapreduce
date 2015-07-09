We welcome your contribution to Cloud MapReduce. If you are interested in helping, read the following on how to proceed. We follow the common practice of other open source projects.

# Basic procedure #

Before you start, send a message to the [CloudMapReduce google group](http://groups.google.com/group/cloudmapreduce), or file a bug report in the Issues database. Describe your proposed changes and check that they fit in with what others are doing and have planned for the project. If you do not have an idea where you can contribute, check out the [roadmap](Roadmap.md) page for ideas we are planning to implement and grab things from the list to take a look at (you should still email to the google group so that folks are aware of who is working on what).

Check out the code and modify the source code to make the changes necessary. Make sure that

  * All public classes and methods should have informative Javadoc comments.

  * Contributions should pass existing unit and integration tests.

Please don't

  * reformat code unrelated to the bug being fixed: formatting changes should be separate patches/commits.
  * comment out code that is now obsolete: just remove it.
  * insert comments around each change, marking the change: folks can use subversion to figure out what's changed and by whom.
  * make things public which are not required by end users.

Please do:

  * try to adhere to the coding style of files you edit;
  * comment code whose function or rationale is not obvious;

Once you are ready, generate a patch using standard diff commands. Detailed commands to use will be posted here soon.

Attach the patch to the issue in the issues database. One of the committers will check out the changes and commit/reject (with explanation) as necessary.

After you have contributed several patches and clearly demonstrate your understanding of the code base and your willingness to contribute to the project for the long term, we will grant you committer access, so that you can commit to the repository directly.
