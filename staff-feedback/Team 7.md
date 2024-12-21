# Milestone M3: Team Feedback

This milestone M3 is the culmination of your SwEnt journey, and it gives us the final opportunity to give you, as a team, formal feedback on how you performed in the project. By now, you should be capable of demonstrating a solid command of the Scrum methodology and collaborative teamwork, and be able to deliver a high-quality, application that is ready for real users.
This feedback report is meant to complement the informal, ungraded feedback that you received from your coaches during the weekly meetings, over email, on Discord, etc.

You can find the evaluation criteria in the [M3 Deliverables](https://github.com/swent-epfl/public/blob/main/project/M3.md) document.
As mentioned before, the standards for M2 were elevated relative to M1, and this progression continued into M3: we now hold you to the highest professional standard in SwEnt.

For this milestone, we looked at several aspects, grouped as follows:

- Application
  - [Completeness](#app-completeness)
  - [Functionality](#app-functionality)
  - [User Experience](#app-user-experience)
- [Design and Implementation](#design-and-implementation)
- [Testing and CI Pipeline](#testing-and-ci-pipeline)
- Process
  - [Documentation](#process-documentation)
  - [Autonomy](#process-autonomy)

## App: Completeness

We first evaluated the depth and complexity of the main __epics__ in your app, along with their contribution to the app, the tangible value they provide to the user, and their alignment with the app’s goals.
We evaluated the extent to which your app meets the __course requirements__ articulated at the start of the semester, and whether they are implemented effectively, they integrate seamlessly, and are indeed essential to the app.
We then looked at the __robustness and completeness__ of the different features you implemented: are all the features finished and polished, are they secure and bug-free, and are they thoughtfully designed.


We did not find a link to your APK so we took the latest build from main. 

**Epics implementation**
You have implemented really well your epics. Great job. Your app contains multiple features and they are fully implemented and serves the app's goals. They also require complex features. 

**App requirements**
You have fulfilled all the app requirements. The interaction between your users is well-made, supports multiple users and use cloud service. You used well your phone sensor with the multiple features including the camera for example.
The offline mode also works very well. Great job!

**Features complexity and completeness**
All features are fully implemented and finished. There were some small bugs and a crash (You can find the details below). Nevertheless, overall the implementations are thoughtfully designed.




For this part, you received 7.1 points out of a maximum of 8.0.

## App: Functionality

In this context, we assessed your app's ability to __handle unexpected inputs__ provided by clueless or malicious users (including spamming buttons, entering wrong inputs, stopping a process mid-way, etc.); we wanted to see that your app handles all edge cases gracefully, has comprehensive error handling, and includes robust mechanisms for maintaining stability under stress.

We then evaluated the performance and reliability of the final product, i.e., __the APK__: we wanted to see that your APK is stable and delivers excellent performance, the UI responds quickly and has seamless navigation.

Next we looked into your implementation of __user authentication and multi-user support__: does the app correctly manage users, can users personalize their accounts, does the app support session persistence, are multi-user interactions well supported, can a user recover a lost password, can accounts be used on another device, and is account information preserved when switching devices.


**App resilience**
When testing the app, we encountered one crash that is described below. Apart from that, most of the buttons that were not working had toasts to inform the user of what was happening. The app is generally resilient, with comprehensive error handling for edge cases and unexpected inputs.

**APK Functionality and Performance**
Some comments about the testing of the app:
- When wanting to try to add a new task with the voice option, we get an error "Recognition error, please try again". 
- When clicking on the PDF conerter widget, there is no action triggered.
- When cropping the image, it would be nice to have the option to share it directly since the option is given when wanting to take a picture to post.
- When saving a picture on a file, it is stored as an id. You could maybe choose another intuitive name. But it is very good that you can modify the name. However, when wanting to download it, the app crashes.
- In the time table, when creating a task linked to a todo, clicking on "Mark as done" updates the UI and state only after reuploading the UI
- There is a typo when wanting to post an image, the button is "Pos" instead of "Post".
- In the feed page, the pictures take a lot of time to upload. The database fetches can be improved to make them faster.
- The sharing feature is really well made. 
- There is also some UI misalignmenets, for example when clicking on the a picture taken, the "Close" text is cropped.
All those comments are principally issues but the app is extremely well done. The features are impressive and most of them are complete. Excellent job!

**Account management**
Users can create and manage accounts. The app supports multi-user interactions and session persistence. So good job on that


For this part, you received 7.2 points out of a maximum of 8.0.

## App: User Experience

For this part, we wanted to see how __intuitive and user-friendly__ the app is for real users. Beyond having good usability, did you pay attention to streamlining the interactions, is it easy to figure out, can new users start making good use of the app quickly, are the interaction flows well thought out and refined.


The user experience is very good. The app is intuitive most of the time, with polished UI/UX design. New users are able to use the app effectively really quickly. 


For this part, you received 1.8 points out of a maximum of 2.0.

## Design and Implementation

We evaluated whether __your code is of high quality and employs best practices__, is it modular and robust, etc.
We expect the codebase to be polished, well documented, follow consistent conventions, be modular, and allow for easy modifications.
You should be able to employ advanced techniques by now, such as asynchronous functions (flows, coroutines), good resource management, and automated dependency injection (e.g., with Hilt).

We assessed your overall __app architecture and design__, looking in particular at aspects surrounding robustness and scalability.
We looked at both the codebase and the documentation of the app (Wiki and architecture diagram).
We expect your design to demonstrate thoughtful consideration for performance, maintainability, and future growth.


**Code quality and Best Practices**
Your followed well the MVVM architecture and followed generally good coding conventions. The codebase is mostly well-commented and is modular. You also took into account the feedback from previous milestones.

**App Architecture and Overall Design**
The architecture is robust, mostly scalable and adheres to best practices. You took into account performance for some cases. Good job on that



For this part, you received 7.2 points out of a maximum of 8.0.

## Testing and CI Pipeline

The first aspect we looked at here was your __test suite__, in terms of both quality and the final line coverage.
We expect testing the be rigorous and to cover all components and edge cases, and they should validate every significant user journey.
Line coverage should be getting close to 90%.
Your end-to-end tests should be detailed and include error-handling scenarios.
The tests should be well-documented and easy to maintain.
Finally, your test suite should demonstrate  advanced techniques, mock data for performance testing, and automated regression tests.

We then considered the quality of your __repository setup and the CI pipeline__, and how professional it is and how easy it is for new developers to bring contributions to the project.
We expect your repository to have a logical structure, use consistent naming, and take full advantage of CI (code quality checks, linting, formatting, etc.)
Ideally, you should also have automated performance testing, deployment pipelines, and the CI should provide detailed feedback for developers.


**Tests**
You have 4 end to end tests. You took into account the good cases and the bad. It is good to have one user flow where a potential user uses the app, makes some mistakes and gets back on the "right" track to use the features you're testing. But it is good that you took the M2 feedback into account.

**Repo Setup and CI Pipeline**
The structure has a logical structure and your CI pipeline is well configured with basic features.
Concerning your repo, avoid folders names like "fake". It is not particularly intuitive and self-explanatory. The repository should be highly professional with a logical structure and consistent naming.




For this part, you received 6.6 points out of a maximum of 8.0.

## Process: Documentation

We looked at your `README` and GitHub Wiki to evaluate the quality and completeness of __your app’s documentation__. We expect the README and Wiki to be thorough and achieve professional-level clarity and completeness.
They should provide detailed descriptions of the app's architecture, implementation of the features, and the development setup.
We also assessed __your use of Figma and the architecture diagram__ for effective UI design, organization, and app structure planning.
By this stage, we expect your Figma to be complete, up-to-date, and to include UI elements for future features, showing foresight and organization.
The architecture diagram should be comprehensive, providing a clear and detailed overview of the app structure and dependencies.
The architecture should be robust, scalable, and optimized for performance, security, and future development.


**README and GitHub Wiki**
The README and wiki are well made. To go even further, the main goal is to offer detailed descriptions of the app's architecture, feature implemenations. Adding additional resources such as app walkthrough videos, demo GIFs is even better. Nevertheless, the development setup to use the code is very useful. It's a good thing to add it in the README !

**Figma and Architecture Diagram**
For the Figma, some screens do not reflect the new UI of the app (For example, the PDF generator page and there is no AI Assistant in the widgets for example). 
Your architecture diagram is comprehensive, providing a clear and detailed overview of the app structure. It is well thought out. Good job




For this part, you received 3.3 points out of a maximum of 4.0.

## Process: Autonomy

A primary goal of SwEnt is to teach you how to __function autonomously as a team__.
For this part of the evaluation, we assessed you team’s independence, spanning Sprint 6 to Sprint 10, based on the meetings with coaches, Sprint planning, and how you managed risk.
By this stage, coaches should no longer be necessary for the team to operate, i.e., you can organize yourselves, you don't need to be reminded about tasks, and you can conduct the Scrum ceremonies on your own.


You showed improved autonomy even though the flow of the meetings had some friction at the start. The discussions afterwards were thoughtful and insightful. Good job on that !


For this part, you received 1.2 points out of a maximum of 2.0.

## Summary

Based on the above points, your intermediate grade for this milestone M3 is 5.30.

The entire SwEnt staff wishes you the very best in your career, and we look forward to seeing you do great things with what you learned this semester.