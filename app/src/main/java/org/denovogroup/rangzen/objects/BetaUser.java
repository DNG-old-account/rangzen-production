package org.denovogroup.rangzen.objects;

/**
 * Created by Liran on 12/13/2015.
 */
public class BetaUser {

    private int position;
    private String name;
    private String[] friends;

    public BetaUser(int position, String name, String[] friends) {
        this.position = position;
        this.name = name;
        this.friends = friends;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getFriends() {
        return friends;
    }

    public void setFriends(String[] friends) {
        this.friends = friends;
    }

    @Override
    public String toString() {
        return name+" ("+friends.length+")";
    }
}
