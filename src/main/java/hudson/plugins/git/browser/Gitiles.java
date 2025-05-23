package hudson.plugins.git.browser;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.plugins.git.GitChangeSet;
import hudson.plugins.git.GitChangeSet.Path;
import hudson.plugins.git.Messages;
import hudson.scm.RepositoryBrowser;
import hudson.util.FormValidation;
import hudson.util.FormValidation.URLCheck;

import java.io.IOException;
import java.io.Serial;
import java.net.URISyntaxException;
import java.net.URL;

import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.servlet.ServletException;

import net.sf.json.JSONObject;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * @author Manolo Carrasco Moñino
 */
public class Gitiles extends GitRepositoryBrowser {

    @Serial
    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public Gitiles(String repoUrl) {
        super(repoUrl);
    }

    // https://gwt.googlesource.com/gwt/+/d556b611fef6df7bfe07682262b02309e6d41769%5E%21/#F3
    @Override
    public URL getDiffLink(Path path) throws IOException {
        URL url = getUrl();
        return new URL(url + "+/" + path.getChangeSet().getId() + "%5E%21");
    }

    // https://gwt.googlesource.com/gwt/+blame/d556b611fef6df7bfe07682262b02309e6d41769/dev/codeserver/java/com/google/gwt/dev/codeserver/ModuleState.java
    @Override
    public URL getFileLink(Path path) throws IOException {
        URL url = getUrl();
        return encodeURL(new URL(url + "+blame/" + path.getChangeSet().getId() + "/" + path.getPath()));
    }

    @Override
    public URL getChangeSetLink(GitChangeSet changeSet) throws IOException {
        URL url = getUrl();
        return new URL(url + "+/" + changeSet.getId() + "%5E%21");
    }


    @Extension
    @Symbol("gitiles")
    public static class ViewGitWebDescriptor extends Descriptor<RepositoryBrowser<?>> {
        @NonNull
        public String getDisplayName() {
            return "gitiles";
        }

        @Override
        public Gitiles newInstance(@NonNull StaplerRequest2 req, @NonNull JSONObject jsonObject) throws FormException {
            return req.bindJSON(Gitiles.class, jsonObject);
        }

        @RequirePOST
        public FormValidation doCheckRepoUrl(@AncestorInPath Item project, @QueryParameter(fixEmpty = true) final String repoUrl)
                throws IOException, ServletException, URISyntaxException {

            String cleanUrl = Util.fixEmptyAndTrim(repoUrl);
            if(initialChecksAndReturnOk(project, cleanUrl)){
                return FormValidation.ok();
            }
            if (!validateUrl(cleanUrl)) {
                return FormValidation.error(Messages.invalidUrl());
            }
            return new URLCheck() {
                protected FormValidation check() throws IOException {
                    String v = cleanUrl;
                    if (!v.endsWith("/"))
                        v += '/';

                    try {
                        // gitiles has a line in main page indicating how to clone the project
                        if (findText(open(new URL(v)), "git clone")) {
                            return FormValidation.ok();
                        } else {
                            return FormValidation.error("This is a valid URL but it doesn't look like Gitiles");
                        }
                    } catch (IOException e) {
                        if (e.getMessage().equals(v)) {
                            return FormValidation.error("Unable to connect " + v, e);
                        } else {
                            return FormValidation.error(e.getMessage(), e);
                        }
                    }
                }
            }.check();
        }
    }
}
