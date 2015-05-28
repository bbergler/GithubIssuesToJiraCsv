import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import com.jcabi.github.*;
import com.thoughtworks.xstream.XStream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Created by bernd on 5/7/15.
 */
public class Runner {
    public static class EstimationEntry{
        String name;
        int issue;
        int priority;
        String description;
        String platform;
        private String version;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("name", name)
                    .add("issue", issue)
                    .add("priority", priority)
                    .add("platform", platform)
                    .toString();
        }

        String[] toArray(){
            String[] output = new String[4];
            output[0]=platform;
            output[1]=name;
            output[2]=description;
            output[3]=version;
            return output;
        }
    }


    public static void main(String[] args) throws IOException {

        FileInputStream fileInput = new FileInputStream(new File("github.properties"));
        Properties properties = new Properties();
        properties.load(fileInput);
        Github github = new RtGithub(properties.getProperty("secret"));
        Repo repo = github.repos().get(new Coordinates.Simple("hoeflehner", "SummitLynx-iPhone"));
        Repo repoAndroid = github.repos().get(new Coordinates.Simple("hoeflehner", "SummitLynx-Android"));
        Map<String,String> query = new HashMap<>();
        query.put("milestone","7");
        query.put("state", "open");
        query.put("labels", "Prio 1");


        List<Issue> listAndroid= FluentIterable.from(repoAndroid.issues().iterate(query)).toList();
        List<Issue> listIOS= FluentIterable.from(repo.issues().iterate(query)).toList();
        List<EstimationEntry> entries = new ArrayList<>();



        for (Issue issue : listIOS) {
            EstimationEntry entry = new EstimationEntry();
            Issue.Smart issueSmart = new Issue.Smart(issue);
            entry.name=issueSmart.title();
            entry.issue=issueSmart.number();
            for (Label label : issueSmart.labels().iterate()) {
                if(label.name().contains("Prio")){
                    entry.priority=Integer.parseInt(label.name().split(" ")[1]);
                }
            }
            StringBuilder builder = new StringBuilder();
            builder.append(issueSmart.htmlUrl().toString());
            builder.append("\n\n");
            builder.append(entry.priority);
            builder.append(issueSmart.body());
            entry.description=builder.toString();
            entry.version=((JsonObject)issue.json().get("milestone")).getString("title");
            entry.platform="iOS";

            entries.add(entry);
        }

        for (Issue issue : listAndroid) {
            EstimationEntry entry = new EstimationEntry();
            Issue.Smart issueSmart = new Issue.Smart(issue);
            entry.name=issueSmart.title();
            entry.issue=issueSmart.number();
            for (Label label : issueSmart.labels().iterate()) {
                if(label.name().contains("Prio")){
                    entry.priority=Integer.parseInt(label.name().split(" ")[1]);
                }
            }
            StringBuilder builder = new StringBuilder();
            builder.append(issueSmart.htmlUrl().toString());
            builder.append("\n\n");
            builder.append(entry.priority);
            builder.append(issueSmart.body());
            entry.description=builder.toString();

            entry.platform="Android";

            entries.add(entry);
        }
        System.out.println(listIOS.size());
        CSVWriter writer = new CSVWriter(new FileWriter("output.csv"), ',');
        writer.writeNext("Component,Summary,Description,FixVersion".split(","));

        for (EstimationEntry entry : entries) {
                writer.writeNext(entry.toArray());
        }

        writer.close();

    }

}

