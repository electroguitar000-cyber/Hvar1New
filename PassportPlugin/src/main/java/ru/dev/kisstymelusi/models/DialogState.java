package ru.dev.kisstymelusi.models;

import org.bukkit.entity.Player;

public class DialogState {

    private Player player;
    private DialogStep step;
    private String fullName;
    private int age;
    private String gender;
    private String married;
    private boolean editMode;

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public DialogStep getStep() {
        return step;
    }

    public void setStep(DialogStep step) {
        this.step = step;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMarried() {
        return married;
    }

    public void setMarried(String married) {
        this.married = married;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
}