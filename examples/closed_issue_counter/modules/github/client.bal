import ballerina/http;

final string GIT_GRAPHQL_API_URL = "https://api.github.com/graphql";
final string EMPTY_STRING = "";

public client class Client {

    string accessToken;
    http:Client githubGraphQlClient;

    public isolated function init(Configuration config) returns error? {
        self.accessToken = config.accessToken;
        self.githubGraphQlClient = check new(GIT_GRAPHQL_API_URL, config.clientConfig);
    }

    remote isolated function getRepositoryIssueList(string repositoryOwnerName, string repositoryName,
                                                    string date, string? label = ()) returns json|error {
        return getRepositoryIssueList(repositoryOwnerName, repositoryName, date,
                        self.accessToken, self.githubGraphQlClient, label);
    }
}

isolated function getRepositoryIssueList(string repositoryOwnerName, string repositoryName,
                                         string date, string accessToken, http:Client graphQlClient,
                                         string? label) returns json|error {
    string stringQuery = getFormulatedStringQueryForGetIssueList(repositoryOwnerName, repositoryName, "closed",
                                                                 date, label);
    http:Request request = new;
    request.setHeader("Authorization", "token " + accessToken);
    json convertedQuery = check stringQuery.fromJsonString();

    request.setJsonPayload(convertedQuery);
    http:Response response = check graphQlClient->post(EMPTY_STRING, request);
    return check response.getJsonPayload();
}

isolated function getFormulatedStringQueryForGetIssueList(string repositoryOwnerName, string repositoryName,
                                                          string state, string date, string? labelName) returns string {
    string query;
    if (labelName is string) {
       query = string `is:${state} is:issue label:${labelName} closed:${date}..${date} repo:${repositoryOwnerName}/${repositoryName}`;
    } else {
       query = string `is:${state} is:issue closed:${date}..${date} repo:${repositoryOwnerName}/${repositoryName}`;
    }
    return "{\"query\": \"query {\n" +
           "search(query: \\\"" + query + "\\\", type: ISSUE, first: 100) {\n" +
           "   issueCount\n" +
           "   }\n" +
           "}\"}";
}

public type Configuration record {
    http:ClientConfiguration clientConfig = {};
    string accessToken;
};