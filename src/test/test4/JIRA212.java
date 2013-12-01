package test4;

public class JIRA212 {
    public Long id;
    public String name;
    public void test() {
        JIRA212 object = new JIRA212();

        object.name = "test";  // get or set string value

        try { // in try
            object.id = 100L;  // get or set long value
            String name = object.name;  // get or set string value
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
