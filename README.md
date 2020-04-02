# jira-service-desk-linked-issues
List of linked customer requests within customer request in Customer Portal on Jira Service Desk

- Only issue links within the project are shown.
- Ignores whether user has permssion to view the linked request or not. (user will be redirected to main page after clicking on non permitted request)
- User must be a customer or have permission to create links to see the links.
- Service desk requestType icons are used
- Customer requests statuses are mapped according to service desk configuration.

![Screenshot](screenshot.png?raw=true "Screenshot")

## TODO:
- css
- logo
- permissions deprecation
- cleanup SdLinksModel
- more control over permissions
- more control over what projects are enabled
