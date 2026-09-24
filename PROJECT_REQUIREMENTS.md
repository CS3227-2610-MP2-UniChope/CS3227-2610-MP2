In this project, you will develop a production-level app with multiple user roles for a formal setting, using basic agentic SE features as part of the software development process.
For example, an app for running a local business may include the following roles and features.
- Owner: Holds full access to all app features, store data, financial settlements, marketing tools, and the ability to add or manage team roles across multiple outlets.
- Store Manager: Oversees day-to-day store operations and can set up or manage staff access, though sensitive legal and financial data may be restricted compared to an owner.
- Cashier: Features limited access meant strictly for handling daily transactions, viewing live orders, and tracking basic daily sales.
For such an app to be at the production-level, it should be robust and reliable for public users, featuring CI/CD pipelines, automated testing, and monitoring rather than just working code.
It is up to you to define the exact features for each user role.
However, in general, if there are N (= 2 or 3) students in your team, there should be N different user roles with the relevant features.
The user interface should be kept simple and separate for each user role. All shared components (e.g., data storage) should be properly designed (e.g., with SRP and DRY).
Each of the student should complete all the features related to a specific user role and a good portion of the work at the team-level. The estimate individual workload should be 1.5 times the level of MP1.
In addition, as you work on the project, you should reflect on the use of basic Agentic SE and create a reflection document.
Here are some sample questions that you can use to guide your reflections.
- What tasks were the AI agent customized to perform and how to determine the appropriate skill set for each task?
- How did you define a specific skill and make sure that it is working?
- What tasks were handled effectively by the agent and help to improve productivity, code quality, or testing efficiency?
- Where did the agent require additional guidance or correction? Were there situations where using the agent created additional work rather than reducing it?
- What would you change in the agent's instructions or skill set if you repeated the task?  What additional skills or tools would make the agent more useful?
- What did you learn about designing an effective single AI agent for software engineering tasks?
 
This is a team project (i.e., no individual submissions allowed). The team size is 2 or 3 students.
You are expected to focus on using basic Agentic SE features (e.g., customizing a single AI-agent and making sure that skill set functions properly) for the implementation and testing tasks in in the development process.
You can’t reuse your MP1 in this MP.
The app should still be a Java desktop app, and the default Java version is Java SE 25.
In addition to Codex, you may choose to use Claude for this project.
You will be asked to redo the project if you violate such restrictions.
If you are unsure, please use the forum to clarify.
 
For us to grade this assignment in a timely manner, we need you to adhere strictly to the following submission guidelines. They will help us grade the assignment in an appropriate manner. You will be penalized if you do not follow these instructions. 
The repo should be named as CS3227-2610-MP2 in a github organization for you team and set to be public. The github organization should be named as CS3227-2610-MP2-[your-project-name] (e.g., CS3227-2610-MP2-BizPartner).
The repo should contain:
- Any source code (src/…). This folder should contain all the codes used in your project. The code will be reviewed for code quality. (Please format it nicely. We would really appreciate it.)
- The latest release of your app.  This should be created as a formal production release in Github as you have done in CS2103/T. The jar file should be generated using Gradle (or some other suitable tools) with the JavaFX third-party libraries included. Make sure your jar file is compatible with all different OSs. Feel free to ask your classmates (e.g., via the forum) or your TAs to test it for you on a specific OS.
- A user guide (docs/UserGuide.md). This should describe all current features of your system, and how the users can set up and test your system. Ensure those descriptions match the product precisely, as it will be used by peer testers (inaccuracies will be considered bugs).
- A developer guide (docs/DeveloperGuide.md). This should describe the design of your system and the relevant software engineering process. It should match the latest release of the product and include an acknowledgement section citing all ideas/code/documentation you have reused.
- A product website. This should be set up using Github pages as you have done in CS2103/T.
- A reflection document (docs/Reflections.md). This should contain your reflections on doing basic Agentic SE. Give at least 3 examples of interesting skills and explain them in detail.
- A folder of summary logs (logs/…). This folder should contain summaries of all the prompts used and the interactions with the AI agents that took place during the development of this app. Ask AI to produce the summaries for you, but don’t forget to verify the correctness of the generated summaries. (These summaries will come in handy when you reflect on your project.) 
Make sure that your repo is named and structured as explained in this write up, and master branch is up to date. We will pull the latest version of your master branch before the deadline, which is 29 Sep (Tue), 2pm SGT, and use it for grading.
One member in your team should also submit the Github organization name, together with the Github usernames of all the team members through the relevant Canvas quiz by 4 Sep (Fri), 2pm SGT.
There absolutely will be no extensions to the deadline of this assignment. Read the [Grading](https://canvas.nus.edu.sg/courses/99226/pages/grading) page in Canvas if you are unsure about the late submission penalty.
In addition, we will also access your repo on Github during the grading process so please make sure that the repo is accessible and there are no further changes to it after your submission.
 
The grading criteria for the assignment are tentatively:
- 20% Features
- 25% Code Quality
- 10% Documentation Quality
- 20% Software Engineering Practices (e.g., project management, design and testing)
- 25% Reflections on Agentic SE