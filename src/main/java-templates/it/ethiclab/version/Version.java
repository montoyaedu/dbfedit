package it.ethiclab.version;

public final class Version {
    public static final String VERSION = "${project.version}";
    public static final String GROUPID = "${project.groupId}";
    public static final String ARTIFACTID = "${project.artifactId}";
    public static final String REVISION = "${git.commit.id}";

    public static String versionString() {
        return GROUPID + ":" + ARTIFACTID + ":" + VERSION + ", rev. " + REVISION;
    }
}