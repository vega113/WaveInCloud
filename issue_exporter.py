#!/usr/bin/python
#
# Copyright 2011 James Purser
# Copyright 2011 Christian Ohler
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


""" This script imports issues from Code.Google and builds a csv file.
The intent is to imported the csv file into JIRA.

In order to run the script you will need the following library:

GData: http://code.google.com/p/gdata-python-client/"""

import gdata.projecthosting.client

project_name = "wave-protocol"

client = gdata.projecthosting.client.ProjectHostingClient()

f = open("issues.csv", "w")
# First row with column titles
f.write("Title,Id,Type,Priority,Status,Description\n")

def get_comments(issue_id):
    comments = []
    while True:
        comments_query = gdata.projecthosting.client.Query(start_index=len(comments) + 1)
        comments_feed = client.get_comments(project_name, issue_id, query=comments_query)
        if not comments_feed.entry:
            break
        comments.extend(comments_feed.entry)
    return comments

def escape(s):
    return '"' + s.encode('utf-8').replace('"', '""') + '"'

def get_id(issue):
    return issue.get_id().split('/')[-1]

def get_link(issue):
    return "http://code.google.com/p/%s/issues/detail?id=%s" % (project_name, get_id(issue))

def get_description(issue, comments):
    # TODO: The content seems to be HTML, I'm not sure if that's what JIRA expects.
    description = issue.content.text
    description += "\n\n---\nIssue imported from %s\n\n" % issue_link
    if issue.owner:
        description += "Owner: %s\n" % issue.owner.username.text
    for cc in issue.cc:
        description += "Cc: %s\n" % cc.username.text
    for label in issue.label:
        description += "Label: %s\n" % label.text
    if issue.stars:
        description += "Stars: %s\n" % issue.stars.text
    if issue.state:
        description += "State: %s\n" % issue.state.text
    if issue.status:
        description += "Status: %s\n" % issue.status.text
    for comment in comments:
        description += "---\n%s:\n%s\n\n" % (comment.title.text, comment.content.text)
        updates = comment.updates
        if updates:
            if updates.summary:
                description += "Summary: %s\n\n" % updates.summary.text
            if updates.status:
                description += "Status: %s\n\n" % updates.status.text
            if updates.ownerUpdate:
                description += "OwnerUpdate: %s\n\n" % updates.ownerUpdate.text
            for label in updates.label:
                description += "Label: %s\n\n" % label.text
            for ccUpdate in updates.ccUpdate:
                description += "CcUpdate: %s\n\n" % ccUpdate.text
    return description


issues_total = 0
while True:
    issues_query = gdata.projecthosting.client.Query(start_index=issues_total + 1)
    issues_feed = client.get_issues(project_name, query=issues_query)
    if not issues_feed.entry:
        break
    for issue in issues_feed.entry:
        issue_id = get_id(issue)
        issue_link = get_link(issue)
        print "issue %s: %s" % (issue_id, issue_link)
        comments = get_comments(issue_id)
        print "issue %s: %s comments" % (issue_id, len(comments))
        # Append one row per issue
        # Title
        row = escape(issue.title.text) + ","
        # Id, e.g. http://code.google.com/feeds/issues/p/wave-protocol/issues/full/1
        row += escape(issue.id.text) + ","
        type = "";
        priority = ""; 
        # There are any number of labels, but we are only interested in two.  Namely Type-* and Priority-*
        for label in issue.label:
            if label.text.startswith("Type-"):
                type = label.text[5:]
            elif label.text.startswith("Priority-"):
                priority = label.text[9:]
        row += escape(type) + ","
        row += escape(priority) + ","
        # Status, e.g. Fixed
        row += escape(issue.status.text) + ","
        # TODO: figure out how to pull the relevant attributes out of Owner, Author
        #row += issue.owner + ","     # type gdata.projecthosting.data.Owner
        #row += issue.author[0] + "," # type atom.data.Author

        description = get_description(issue, comments)
        print description

        row += escape(description)
        f.write(row + "\n")
    issues_total += len(issues_feed.entry)

f.close()
print "%s issues total" % issues_total
