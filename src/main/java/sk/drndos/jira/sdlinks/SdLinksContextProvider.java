package sk.drndos.jira.sdlinks;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import com.atlassian.servicedesk.api.permission.ServiceDeskPermissionService;
import com.atlassian.servicedesk.api.request.CustomerRequest;
import com.atlassian.servicedesk.api.request.ServiceDeskCustomerRequestService;
import com.atlassian.servicedesk.api.requesttype.RequestType;
import com.atlassian.servicedesk.api.requesttype.RequestTypeService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class SdLinksContextProvider implements ContextProvider {

  private static final String KEY_ISSUE = "issue";

  private static final Logger log = LogManager.getLogger("sdlinks");
  private final PermissionManager permissionManager;
  private final IssueLinkManager issueLinkManager;
  private final ServiceDeskPermissionService serviceDeskPermissionService;
  private final RequestTypeService requestTypeService;
  private final ServiceDeskCustomerRequestService serviceDeskCustomerRequestService;


  public SdLinksContextProvider(PermissionManager permissionManager,
      IssueLinkManager issueLinkManager, ServiceDeskPermissionService serviceDeskPermissionService,
      RequestTypeService requestTypeService,
      ServiceDeskCustomerRequestService serviceDeskCustomerRequestService) {
    super();
    this.permissionManager = permissionManager;
    this.issueLinkManager = issueLinkManager;
    this.serviceDeskPermissionService = serviceDeskPermissionService;
    this.requestTypeService = requestTypeService;
    this.serviceDeskCustomerRequestService = serviceDeskCustomerRequestService;
  }

  public List<SdLinksGroupsModel> getIssueLinksGroups(CustomerRequest customerRequest, ApplicationUser user) {

    Issue issueObject = customerRequest.getIssue();
    List<SdLinksGroupsModel> data = new ArrayList<>();

    if (permissionManager.hasPermission(Permissions.LINK_ISSUE, issueObject, user)
        || serviceDeskPermissionService.isCustomer(user, issueObject)) {
      Collection<IssueLink> outwardIssueLinks = issueLinkManager.getOutwardLinks(issueObject.getId());
      Collection<IssueLink> inwardIssueLinks = issueLinkManager.getInwardLinks(issueObject.getId());
      List<SdLinksGroupsModel> dataInward = inwardIssueLinks.stream()
          .filter(issueLink -> Objects.equals(issueLink.getSourceObject().getProjectId(), issueObject.getProjectId()))
          .map(issueLink -> processLink(user, issueLink.getSourceObject(), issueLink.getIssueLinkType().getInward()))
          .collect(Collectors.groupingBy(SdLinksModel::getLinkName))
          .entrySet().stream()
          .map(entry -> SdLinksGroupsModel.builder().groupName(entry.getKey()).links(entry.getValue()).build())
          .collect(Collectors.toList());
      List<SdLinksGroupsModel> dataOutward = outwardIssueLinks.stream()
          .filter(
              issueLink -> Objects.equals(issueLink.getDestinationObject().getProjectId(), issueObject.getProjectId()))
          .map(issueLink -> processLink(user, issueLink.getDestinationObject(),
              issueLink.getIssueLinkType().getOutward()))
          .collect(Collectors.groupingBy(SdLinksModel::getLinkName))
          .entrySet().stream()
          .map(entry -> SdLinksGroupsModel.builder().groupName(entry.getKey()).links(entry.getValue()).build())
          .collect(Collectors.toList());
      data.addAll(dataInward);
      data.addAll(dataOutward);

    } else {
      log.warn("Get links request ignored");
    }
    return data;
  }

  private SdLinksModel processLink(ApplicationUser user, Issue lIssue, String issueLinkName) {
    CustomerRequest customerRequest = customerRequest(user, lIssue.getId());
    RequestType requestType = requestTypeService.getRequestTypes(
        user,
        requestTypeService.newQueryBuilder()
            .serviceDesk(customerRequest.getServiceDeskId())
            .requestType(customerRequest.getRequestTypeId())
            .build()).findFirst().get();
    return SdLinksModel.builder()
        .issueKey(lIssue.getKey())
        .issueSummary(lIssue.getSummary())
        .issueTypeDescription(requestType.getDescription())
        .issueTypeIcon(
            "/servicedesk/customershim/secure/viewavatar?avatarType=SD_REQTYPE&avatarId=" + requestType.getIconId())
        .issueTypeName(requestType.getName())
        .linkName(issueLinkName)
        .priorityDescription(
            lIssue.getPriority() != null ? lIssue.getPriority()
                .getDescription() : "")
        .priorityIcon(
            lIssue.getPriority() != null ? lIssue.getPriority()
                .getIconUrl() : "")
        .resolved(lIssue.getResolution() != null)
        .statusName(customerRequest.currentStatus().status())
        .statusDescription(lIssue.getStatus().getDescription())
        .statusStyle(mapStyle(lIssue.getStatus().getStatusCategory().getKey()))
        .build();
  }

  @Override
  public void init(Map<String, String> map) throws PluginParseException {

  }

  private String mapStyle(String key) {
    switch (key) {
      case "new":
        return "new";
      case "done":
        return "success";
      case "indeterminate":
        return "inprogress";
      default:
        return "subtle";
    }
  }

  private CustomerRequest customerRequest(ApplicationUser user, Long issueId) {
    return serviceDeskCustomerRequestService.getCustomerRequest(user,
        serviceDeskCustomerRequestService.newIssueQueryBuilder()
            .overrideSecurity(true)
            .issue(issueId)
            .build()
    );
  }

  @Override
  public Map<String, Object> getContextMap(Map<String, Object> map) {
    final ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    CustomerRequest customerRequest = customerRequest(user, ((Issue) map.get(KEY_ISSUE)).getId());
    map.put("issue_link_groups", getIssueLinksGroups(customerRequest, user));
    return map;
  }
}