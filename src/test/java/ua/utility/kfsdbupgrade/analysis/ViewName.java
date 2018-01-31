package ua.utility.kfsdbupgrade.analysis;

public enum ViewName {
    MASTER("KFS 3 to 6 to 7 Database Upgrade - nonprod"),
    DEVELOP("KFS 3 to 6 to 7 Database Upgrade - development - nonprod"),
    ONBRANCH("KFS 3 to 6 to 7 Database Upgrade on branch - nonprod");


    private String name;

    ViewName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ViewName getViewNameByTitle(String name) {
        for (ViewName viewName : ViewName.values()) {
            if (viewName.getName().equals(name)) {
                return viewName;
            }
        }

        throw new RuntimeException(String.format("Unknown view name!: '%s'", name));
    }
}
