package com.example.brandon.kingoftokyoscratch;

public class Player {
    private String name;
    private String pid; //google participant id
    private int health;
    private int victoryPoint;
    private int energy;
    private boolean inTokyo;

    public Player(String name, String pid) {
        this.name = name;
        this.pid = pid;
        this.health = 10;
        this.victoryPoint = 0;
        this.energy = 0;
        this.inTokyo = false;
    }
    /*
    public Player(int h, int vp, int e){
        health = h;
        victoryPoint = vp;
        energy = e;
    }
*/
    public void takeDamage(int i){
        health -= i;
        if(health < 0){
            health = 0;
        }
    }

    public void updateHealth(int i) {
        if(!inTokyo) {
            health += i;
            if (health > 10) {
                health = 10;
            }
        }
    }

    public void updateVictoryPoint(int v) {
        victoryPoint += v;
    }

    public void updateEnergy(int e) {
        energy += e;
    }

    public String getName() {
        return name;
    }

    public String getPid() {
        return pid;
    }

    public int getHealth() {
        return health;
    }

    public int getVictoryPoint() {
        return victoryPoint;
    }

    public int getEnergy() {
        return energy;
    }

    public boolean getInTokyo(){
        return inTokyo;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void setVictoryPoint(int victoryPoint) {
        this.victoryPoint = victoryPoint;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public void setInTokyo(boolean inTokyo){
        this.inTokyo = inTokyo;
    }
}