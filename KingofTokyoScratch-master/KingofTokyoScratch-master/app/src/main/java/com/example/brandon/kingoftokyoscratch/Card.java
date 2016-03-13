package com.example.brandon.kingoftokyoscratch;

/**
 * Created by Brandon on 5/6/2015.
 */
public class Card {
    private int id; //unique id of card
    private int cost; //energy cost to buy card
    private String name; //Name of the card
    private String description; //String descriptioned on card

    /*
    Flag Code:
    0: happens to no one / ignore
    1: happens to self
    2: happens to all but self
    3: happens to all
     */

    int heartFlag; //flag for health
    int heartDelta; //change in health, negative value for losing health (taking damage)
    int VPFlag; //flag for victory points
    int VPDelta; //change in victory points, negative value for losing VP
    int energyFlag; //flag for energy
    int energyDelta; //change in energy, negative value for losing energy

    public Card(){}

    public Card(int id, int cost, String description, String name) {
        this.id = id;
        this.cost = cost;
        this.description = description;
        this.name = name;
    }

    public Card(int id, int cost, String name, String description, int heartFlag, int heartDelta, int VPFlag, int VPDelta, int energyFlag, int energyDelta) {
        this.id = id;
        this.cost = cost;
        this.description = description;
        this.name = name;
        this.heartFlag = heartFlag;
        this.heartDelta = heartDelta;
        this.VPFlag = VPFlag;
        this.VPDelta = VPDelta;
        this.energyFlag = energyFlag;
        this.energyDelta = energyDelta;
    }

    public int getId() {
        return id;
    }

    public int getCost() {
        return cost;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getHeartFlag() {
        return heartFlag;
    }

    public int getHeartDelta() {
        return heartDelta;
    }

    public int getVPFlag() {
        return VPFlag;
    }

    public int getVPDelta() {
         return VPDelta;
    }

    public int getEnergyFlag() {
         return energyFlag;
    }

    public int getEnergyDelta() {
        return energyDelta;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHeartFlag(int heartFlag) {
        this.heartFlag = heartFlag;
    }

    public void setHeartDelta(int heartDelta) {
        this.heartDelta = heartDelta;
    }

    public void setVPFlag(int VPFlag) {
        this.VPFlag = VPFlag;
    }

    public void setVPDelta(int VPDelta) {
        this.VPDelta = VPDelta;
    }

    public void setEnergyFlag(int energyFlag) {
        this.energyFlag = energyFlag;
    }

    public void setEnergyDelta(int energyDelta) {
        this.energyDelta = energyDelta;
    }
}
