package cu.axel.smartdock.models;

public class Action {
private int icon;
private String text;

    public Action(int icon, String text) {
        this.icon = icon;
        this.text = text;
    }

    public int getIcon() {
        return icon;
    }

    public String getText() {
        return text;
    }
}
