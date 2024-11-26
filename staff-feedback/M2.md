# Milestone M2: Team Feedback

This milestone M2 provides an opportunity to give you, as a team, formal feedback on how you are performing in the project. By now, you should be building upon the foundations set in M1, achieving greater autonomy and collaboration within the team. This is meant to complement the informal, ungraded feedback from your coaches given during the weekly meetings or asynchronously on Discord, email, etc.

The feedback focuses on two major themes:
First, whether you have adopted good software engineering practices and are making progress toward delivering value to your users.
Is your design and implementation of high quality, easy to maintain, and well tested?
Second, we look at how well you are functioning as a team, how you organize yourselves, and how well you have refined your collaborative development.
An important component is also how much you have progressed, as a team, since the previous milestone.
You can find the evaluation criteria in the [M2 Deliverables](https://github.com/swent-epfl/public/blob/main/project/M2.md) document.
As mentioned in the past, the standards for M2 are elevated relative to M1, and this progression will continue into M3.

We looked at several aspects, grouped as follows:

 - Design
   - [Features](#design-features)
   - [Design Documentation](#design-documentation)
 - [Implementation and Delivery](#implementation-and-delivery)
 - Scrum
   - [Backlogs Maintenance](#scrum-backlogs-maintenance)
   - [Documentation and Ceremonies](#scrum-documentation-and-ceremonies)
   - [Continuous Delivery of Value](#scrum-continuous-delivery-of-value)

## Design: Features

We interacted with your app from a user perspective, assessing each implemented feature and flagging any issues encountered. Our evaluation focused mainly on essential features implemented during Sprints 3, 4, and 5; any additional features planned for future Sprints were not considered in this assessment unless they induced buggy behavior in the current APK.
We examined the completeness of each feature in the current version of the app, and how well it aligns with user needs and the overall project goals.


The features implemented in the app are diverse and very well made. You make good use of the camera sensor to interact with other users of the app. You have thought about multiple features and succeeded in delivering impressive result.
The already implemented features are almost complete, bug-free and allow a good user experience. They also bring meaningful value to the app. Excellent job !


For this part, you received 7.7 points out of a maximum of 8.0.

## Design: Documentation

We reviewed your Figma (including wireframes and mockups) and the evolution of your overall design architecture in the three Sprints.
We assessed how you leveraged Figma to reason about the UX, ensure a good UX, and facilitate fast UI development.
We evaluated whether your Figma and architecture diagram accurately reflect the current implementation of the app and how well they align with the app's functionality and structure.


**Figma**
As you said in the wiki, only the mockup is up-to-date. To make this task easier for next sprints, you should gradually adapt it each week. The goal of the Figma is to represent the final state of your app with all features, even those that are not implemented yet. It allows to give the whole team a global overview and the same vision as you go through the weeks. Nevertheless, it took an impressive amount of time to do the mockup. Great job!

**Architecture Diagram**
Concerning the architecture diagram, it provides a clear and precise architecture of the app. The main features of your app are accounted for in it. Good job!


For this part, you received 4.5 points out of a maximum of 6.0.

## Implementation and Delivery

We evaluated several aspects of your app's implementation, including code quality, testing, CI practices, and the functionality and quality of the APK.
We assessed whether your code is well modularized, readable, and maintainable.
We looked at the efficiency and effectiveness of your unit and end-to-end tests, and at the line coverage they achieve.


**Code quality**
You tend to create very large PRs, which can be challenging for both you and your reviewers. As mentioned in most individual feedback, an average PR should ideally contain around 400 lines of code (LOC). Submitting overly large PRs makes it harder for you to ensure that everything is implemented correctly and significantly increases the workload for reviewers, who may overlook issues or miss critical details.
To improve, please break down your tasks into smaller, more manageable units as discussed in M1. Aim to create PRs that correspond to tasks requiring approximately 4 hours to complete.
Your documentation is also inconsistent throughout the classes. Make sure to explain what you're doing as you're coding as this is very helpful for future developers and reviewers.
Nevertheless, your use of asynchronous functions is well done and you mostly adhere to good coding practices, like code modulariy and respect of the MVVM architecture. Great job !

**Tests and CI**
Your project's code coverage is excellent (above 90%). Everything is well-tested and mostly take into account edge cases which is very good.
You have three interesting end to end tests:
- CalculatorWidgetE2ETest: This is a well-structured end to end test. But you're mostly testing the "happy" path where the user only puts in valid values. Try to put edge cases to make sure the code is robust. Also, while navigation is mocked, you could add assertions to ensure that the navigation state is updated correctly when transitioning between screens.
- PomodoroTimerE2ETest: This is a comprehensive test, but it could be further improved by including scenarios that mimic real user behavior, such as:
       - Handling user interactions during transitions (e.g., pausing during a skip).
       - Ensuring the timer state persists if the user navigates away and then returns to the screen.
- SocialInteractionE2ETest: This is an excellent test that shows a complete user flow. To improve even further, you can add tests that mimic interacting with elements that depend on asynchronous data (e.g., search results or publications), particularly handling scenarios where users click multiple times on these elements. You should also test the cases where you search for a non-existent user for example.

These are examples to make your end to end tests even more complete and give you ideas for future implementations. The goal is to make sure your app is robust when faced with both clueless and experimented users. Nevertheless, good job on the work done !

Remark: your E2ETests.kt is confusing and contains multiple tests and fake viewmodels as well as mocks of the firebase calls. Each end to end test should have its own file. Concerning the fake view models, you should put them in a separate file so that everyone in the team can easily access them and avoid code duplication to keep your code maintainable.
You should also use intuitive and self explanatory class or function names. Avoid using names like "FakeNavigationsActions2" as it is not helpful to understand the difference with "FakeNavigationsActions".

**APK Test**
We tested your app and have some remarks and suggestions for improvement:

- Home Page: Consider adding a brief instructional text to guide users on what to do initially. For example, let them know that adding widgets is required, and the current behavior is normal until widgets are added. This can help improve user onboarding.
- Unfinished Features: Displaying a toast message for unfinished features is a great way to communicate with users. Well done! Try to be consistent in that aspect and do the same for all unfinished features.
- Image to PDF: The feature works well, but after completion, a toast message is displayed indicating that the document failed to save. You may want to investigate and resolve this issue.
- Text to PDF: This feature redirects users to the phone's document directory. However, it’s unclear what type of file is required to use this functionality. Adding a brief explanation or instructions would enhance user understanding.
- Calculator: When entering an incorrect expression that results in an undefined output, users are forced to rewrite the entire expression instead of modifying it. The feature is already excellent, but adding the ability to edit incorrect expressions would make it even better.
- Timetable: Adding tasks works perfectly—great job! Linking a task to a to-do and marking it as done updates correctly the to-do screen. Excellent cohesion. You can consider adding a month display in the timetable and a "Today" button to help users quickly navigate back to the current date.
- Folders Screen: When adding the first course, it appears to be added twice. When adding another course, the second instance of the first course is replaced by the new one. This issue may need debugging to ensure accurate behavior.
- Taking Pictures: After taking and saving a picture, the app redirects users to the folders screen, but nothing is added there. It turns out the photo is saved in the Gallery section under profile settings. If this behavior is intentional, consider redirecting users to the Gallery or providing a message to clarify this.
- Video Feed: The video feed loops through existing posts, creating a never-ending feed similar to TikTok. This is an excellent feature if that is your intended design. Well done!

Overall, your app is excellent and demonstrates a high level of commitment and attention to detail. The features are well made, practical, and interesting. This is a genuinely useful app that could greatly benefit students. Great job!


For this part, you received 15.4 points out of a maximum of 16.0.

## Scrum: Backlogs Maintenance

We looked at whether your Scrum board is up-to-date and well organized.
We evaluated your capability to organize Sprint 6 and whether you provided a clear overview of this planning on the Scrum board.
We assessed the quality of your user stories and epics: are they clearly defined, are they aligned with a user-centric view of the app, and do they suitably guide you in delivering the highest value possible.


**Sprint backlog**
Your sprint backlog is very well made. You efficiently use the tags which gives a proper overview of how the sprint will go. Excellent job on that. However, there are multiple tasks that are estimated to take too much time (7 to 10 hours) and that implement both backend and frontend. As said in the meetings, you should try to divide your tasks so that one task doesn't exceed 4-5hours. The backend and frontend should not be done in a same task but should each be independant when taking too long to implement.
Also, don't hesitate to add a description when the title is not descriptive enough. For example, "implement a about screen" doesn't necessarily explain which about screen is being talked about. Is it about the user, the app or the team ?
The sprint planning should also show some risk planning elements. In the description of a task, if you know you will depend on a certain feature or you will need to link a specific screen to some place, then you can add it in the description.
Those are some remarks for you to improve, however the sprint planning shows clearly who will do which feature, how much time it will take and its priority. Good job on following the good conventions.

**Product Backlog**
When finishing a user story, you should put it in the Done column of the sprint where it was finished so that you product backlog is always up-to-date and you know exactly what is left to do.
Another detail is that the "User story" tag is used inconsistently which makes it a bit confusing to differentiate between the user stories and the epics.
Nevertheless, your product backlog is comprehensive and includes all features you want to have in your app. The priority tags and the link between the epics and the user stories is very helpful. Good job!
Don't hesitate to update it whenever you have a new idea of a feature you want to implement so that you have all the information in one place.


For this part, you received 3.6 points out of a maximum of 4.0.

## Scrum: Documentation and Ceremonies

We assessed how you used the Scrum process to organize yourselves efficiently.
We looked at how well you documented your team Retrospective and Stand-Up during each Sprint.
We also evaluated your autonomy in using Scrum.


It has been noted that sprint retrospectives are not always being completed on time, which could indicate that meetings are either not happening or that notes are not being taken. Please ensure that retrospectives are conducted and documented for each sprint to maintain a clear overview of progress and areas for improvement.
Standup meetings should also be held regularly, ideally once a week, at a fixed time that everyone agrees on. Attendance must be mandatory, and all team members should demonstrate progress by showcasing the start or status of their implementations.
Additionally, there seems to be an imbalance in workload distribution, with some team members working more than others. This imbalance can impact team morale and productivity. It's essential to have open discussions as a team to address and resolve this issue.
Finally, during meetings, there is a noticeable lack of proactiveness and engagement from some members. Everyone should actively participate, contribute ideas, and stay engaged throughout the meetings. Improving this aspect will strengthen team collaboration and efficiency in upcoming sprints.

**Important note**: You are not graded on the difficulties you are encountering. It is very well appreciated when you acknowledge you have issues and show that you are actively trying to solve them. The honesty and improvement in the process is what matters for this course, so please, be as sincere as possible in the SCRUM documents and let us know when there are problems.


For this part, you received 2.4 points out of a maximum of 4.0.

## Scrum: Continuous Delivery of Value

We evaluated the Increment you delivered at the end of each Sprint, assessing your team’s ability to continuously add value to the app.
This included an assessment of whether the way you organized the Sprints was conducive to an optimal balance between effort invested and delivery of value.


As a team, you deliver each sprint an impressive amount of work with UI/UX improvements, new implemented features and continuously enhanced code quality. This is exceptional work !


For this part, you received 2 points out of a maximum of 2.0.

## Summary

For M2, your APK was late by 2 hours. Following the late submission penalty for this course, you were penalised for 4% of your final grade. Therefore, your intermediate grade for this milestone M2 is 5.23. If you are interested in how this fits into the bigger grading scheme, please see the [project README](https://github.com/swent-epfl/public/blob/main/project/README.md) and the [course README](https://github.com/swent-epfl/public/blob/main/README.md).

Your coaches will be happy to discuss the above feedback in more detail.

Good luck for the next Sprints!
