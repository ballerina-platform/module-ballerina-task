// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

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

github:Client githubClient = check new (config);

class Job {

    *task:Job;

    public function execute() {
        log:printInfo("Job started to executed.");
        time:Utc date = time:utcNow();
        time:Civil civil = time:utcToCivil(date);
        string months = civil.month.toString();
        if months.length() == 1 {
            months = "0" + months;
        }
        string days = civil.day.toString();
        if days.length() == 1 {
            days = "0" + days;
        }
        string dateInString = (civil.year).toString() + "-" + months + "-" + days;
        string|error closedIssueCounts = getClosedIssueCounts(repoNamesWithLabel, dateInString);
        if closedIssueCounts is string {
            io:println("Closed issue count details on " + dateInString + ":\n" + closedIssueCounts);
        } else {
            log:printError("Failed to get the closed issue count details", 'error = closedIssueCounts);
        }
    }

    isolated function init() {}
}

function getClosedIssueCounts(string[] entries, string date) returns string {
    string totalCount = "";
    foreach string entry in entries {
        string[] orgname = regex:split(entry, ":");
        json|error issueCount;
        if orgname.length() > 1 {
            issueCount = githubClient->getRepositoryIssueList(repositoryOwner, orgname[0], date, orgname[1]);
        } else {
            issueCount = githubClient->getRepositoryIssueList(repositoryOwner, orgname[0], date);
        }
        if issueCount is json {
            json|error value = issueCount.data.search.issueCount;
            if value is json {
                if orgname.length() > 1 {
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
    return totalCount;
}
