import ballerina/io;
import ballerina/log;
import ballerina/regex;
import ballerina/task;
import ballerina/time;
import closed_issue_counter.github;

configurable string github_access_token = ?;
configurable string repositoryOwner = ?;
configurable string[] repoNamesWithLabel = ?;

github:Configuration config = {
    accessToken: github_access_token
};

class Job {

    *task:Job;

    public function execute() {
        io:println("Job started to executed.");
        time:Utc date = time:utcNow();
        time:Civil civil = time:utcToCivil(date);
        string months = civil.month.toString();
        if (months.length() == 1) {
            months = "0" + months;
        }
        string days = civil.day.toString();
        if (days.length() == 1) {
            days = "0" + days;
        }
        string dateInString = (civil.year).toString() + "-" + months + "-" + days;
        string|error colsedIssueCounts = getColsedIssueCounts(repoNamesWithLabel, dateInString);
        if colsedIssueCounts is string {
            io:println("Closed issue count details on " + dateInString + ":\n" + colsedIssueCounts);
        } else {
            log:printError("Error:", colsedIssueCounts);
        }
    }

    isolated function init() {}
}

function getColsedIssueCounts(string[] entries, string date) returns string {
    string totalCount = "";
    github:Client|error githubClient = new (config);
    if (githubClient is github:Client) {
        foreach string entry in entries {
            string[] orgname = regex:split(entry, ":");
            json|error issueCount;
            if (orgname.length() > 1) {
                issueCount = githubClient->getRepositoryIssueList(repositoryOwner, orgname[0], date, orgname[1]);
            } else {
                issueCount = githubClient->getRepositoryIssueList(repositoryOwner, orgname[0], date);
            }
            if (issueCount is json) {
                json|error value = issueCount.data.search.issueCount;
                if (value is json) {
                    if (orgname.length() > 1) {
                        totalCount += orgname[0] + ":" + orgname[1] + " " + value.toString() + "\n";
                    } else {
                        totalCount += orgname[0] + " " + value.toString() + "\n";
                    }
                } else {
                    log:printError("Error:", value);
                }
            } else {
                log:printError("Error:", issueCount);
            }
        }
    } else {
        log:printError("Error:", githubClient);
    }
    return totalCount;
}
