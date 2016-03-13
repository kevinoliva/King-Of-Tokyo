package com.example.brandon.kingoftokyoscratch;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Basic turn data. It's just a blank data string and a turn number counter.
 *
 * @author wolff
 *
 */
public class Turn {

    ArrayList<Player> players;
    boolean isTokyoAttacked;
    String lastAttackerId;
    ArrayList<Card> drawPile;
    ArrayList<Card> discardPile;
    Card displayPile[];

    public Turn() {
        players = new ArrayList<>();
        isTokyoAttacked = false;
        lastAttackerId = "";
        drawPile = new ArrayList<>();
        displayPile = new Card[3];
        discardPile = new ArrayList<>();
    }

    public void addPlayer(String playerName, String playerID) {
        Player tmpPlayer = new Player(playerName, playerID);
        players.add(tmpPlayer);
    }

    public boolean isTokyoAttacked() {
        return isTokyoAttacked;
    }

    public String getLastAttackerId() {
        return lastAttackerId;
    }

    public void setTokyoAttacked(boolean isTokyoAttacked) {
        this.isTokyoAttacked = isTokyoAttacked;
    }

    public void setLastAttackerId(String lastAttackerId) {
         this.lastAttackerId = lastAttackerId;
    }

    public boolean isTokyoEmpty(){
        for (int i = 0; i < players.size(); i++){
            if (players.get(i).getInTokyo()){
                return false;
            }
        }
        return true;
    }

    public void setUpCards(){
        drawPile.add(new Card(drawPile.size()+1, 5, "Apartment Building", "+3 VP", 0, 0, 1, 3, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 4, "Commuter Train", "+2 VP", 0, 0, 1, 2, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 3, "Corner Store", "+1 VP", 0, 0, 1, 1, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 8, "Energize", "+9 Energy", 0, 0, 0, 0, 1, 9));
        drawPile.add(new Card(drawPile.size()+1, 7, "Evacuation Orders", "-5 VP to enemies", 0, 0, 2, -5, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 3, "Fire Blast", "Enemies take 2 damage", 2, -2, 0, 0, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 6, "Gas Refinery", "+2 VP and 3 damage to enemies.", 2, -3, 1, 2, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 3, "Heal", "+2 hearts", 1, 2, 0, 0, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 4, "High Altitude Bombing", "3 damage to all", 3, -3, 0, 0, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 5, "Jet Fighters", "+5 VP and 4 damage", 1, -4, 1, 5, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 3, "National Guard", "+2 VP and 2 damage", 1, -2, 1, -2, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 6, "Nuclear Power Plant", "+2 VP and +3 hearts.", 1, 2, 1, 3, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 6, "Skyscraper", "+4 VP", 0, 0, 1, 4, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 4, "Tanks", "+4 VP and 3 damage.", 1, 3, 1, 4, 0, 0));
        drawPile.add(new Card(drawPile.size()+1, 6, "Amusement Park", "+4 VP", 0, 0, 1, 4, 0, 0));
        shuffleDrawPile();
        placeCard(0);
        placeCard(1);
        placeCard(2);
    }

    //Takes card from draw pile and places on spot num (0, 1, or 2)
    public void placeCard(int num){
        //make sure with in bound
        if (num < 0 || num > 2){
            return;
        }
        //make discard pile into draw pile if draw empty
        if(drawPile.isEmpty()){
            drawPile = discardPile;
            discardPile = new ArrayList<>();
            shuffleDrawPile();
        }
        //draw a card to display in spot num
        displayPile[num] = drawPile.remove(0);
    }

    //randomize drawPile's order
    public void shuffleDrawPile(){
        long seed = System.nanoTime();
        Collections.shuffle(drawPile, new Random(seed));
    }

    //replace a bought or discarded card
    public void replaceCard(int num){
        discardPile.add(displayPile[num]);
        placeCard(num);
    }

    // This is the byte array we will write out to the TBMP API.
    public byte[] persist() {
        JSONObject retVal = new JSONObject();

        try {
            retVal.put("isTokyoAttacked", isTokyoAttacked);
            retVal.put("lastAttackerId",lastAttackerId);

//            JSONObject drawPileVal = new JSONObject();
//            for(int i = 0; i < drawPile.size(); i++){
//                JSONObject drawVal = new JSONObject();
//                drawVal.put("id",drawPile.get(i).getId());
//                drawVal.put("cost",drawPile.get(i).getCost());
//                drawVal.put("name",drawPile.get(i).getName());
//                drawVal.put("description",drawPile.get(i).getDescription());
//                drawVal.put("heartFlag",drawPile.get(i).getHeartFlag());
//                drawVal.put("heartDelta",drawPile.get(i).getHeartDelta());
//                drawVal.put("VPflag",drawPile.get(i).getVPFlag());
//                drawVal.put("VPDelta",drawPile.get(i).getVPDelta());
//                drawVal.put("energyFlag",drawPile.get(i).getEnergyFlag());
//                drawVal.put("energyDelta",drawPile.get(i).getEnergyDelta());
//                drawPileVal.put("DrawCard"+Integer.toString(i),drawVal);
//            }
//            retVal.put("DrawPile",drawPileVal);
//
//            JSONObject displayPileVal = new JSONObject();
//            for(int i = 0; i < 3; i++){ //displayPile
//                if(displayPile[i] != null) {
//                    JSONObject displayVal = new JSONObject();
//                    displayVal.put("id", displayPile[i].getId());
//                    displayVal.put("cost", displayPile[i].getCost());
//                    displayVal.put("name", displayPile[i].getName());
//                    displayVal.put("description", displayPile[i].getDescription());
//                    displayVal.put("heartFlag", displayPile[i].getHeartFlag());
//                    displayVal.put("heartDelta", displayPile[i].getHeartDelta());
//                    displayVal.put("VPflag", displayPile[i].getVPFlag());
//                    displayVal.put("VPDelta", displayPile[i].getVPDelta());
//                    displayVal.put("energyFlag", displayPile[i].getEnergyFlag());
//                    displayVal.put("energyDelta", displayPile[i].getEnergyDelta());
//                    displayPileVal.put("DisplayCard" + Integer.toString(i), displayVal);
//                }
//            }
//            retVal.put("DisplayPile",displayPileVal);
//
//            JSONObject discardPileVal = new JSONObject();
//            for(int i = 0; i < discardPile.size(); i++){
//                JSONObject discardVal = new JSONObject();
//                discardVal.put("id",discardPile.get(i).getId());
//                discardVal.put("cost",discardPile.get(i).getCost());
//                discardVal.put("name",discardPile.get(i).getName());
//                discardVal.put("description",discardPile.get(i).getDescription());
//                discardVal.put("heartFlag",discardPile.get(i).getHeartFlag());
//                discardVal.put("heartDelta",discardPile.get(i).getHeartDelta());
//                discardVal.put("VPflag",discardPile.get(i).getVPFlag());
//                discardVal.put("VPDelta",discardPile.get(i).getVPDelta());
//                discardVal.put("energyFlag",discardPile.get(i).getEnergyFlag());
//                discardVal.put("energyDelta",discardPile.get(i).getEnergyDelta());
//                discardPileVal.put("DiscardCard"+Integer.toString(i),discardVal);
//            }
//            retVal.put("DiscardPile",discardPileVal);

            JSONObject playerPileVal = new JSONObject();
            for(int i = 0; i < players.size(); i++){
                JSONObject playerVal = new JSONObject();
                playerVal.put("name",players.get(i).getName());
                playerVal.put("pid",players.get(i).getPid());
                playerVal.put("heart",players.get(i).getHealth());
                playerVal.put("vp", players.get(i).getVictoryPoint());
                playerVal.put("energy", players.get(i).getEnergy());
                playerVal.put("inTokyo", players.get(i).getInTokyo());
                playerPileVal.put("Player"+Integer.toString(i),playerVal);
            }
            retVal.put("Players",playerPileVal);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String st = retVal.toString();

        Log.d("TURNDATA", "==== PERSISTING\n" + st);

        return st.getBytes(Charset.forName("UTF-8"));
    }

    // Creates a new instance of SkeletonTurn.
    static public Turn unpersist(byte[] byteArray) {

        if (byteArray == null) {
            Log.d("TURNDATA", "Empty array---possible bug.");
            return new Turn();
        }

        String st = null;
        try {
            st = new String(byteArray, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }

        Log.d("TURNDATA", "====UNPERSIST \n" + st);

        Turn retVal = new Turn();

        try {
            JSONObject obj = new JSONObject(st);

            if (obj.has("isTokyoAttacked")) {
                retVal.setTokyoAttacked(obj.getBoolean("isTokyoAttacked"));
            }
            if (obj.has("lastAttackerId")) {
                retVal.setLastAttackerId(obj.getString("lastAttackerId"));
            }

//            if(obj.has("DrawPile")) {
//                JSONObject drawPileObj = obj.getJSONObject("DrawPile");
//                for (int i = 0; i < drawPileObj.length(); i++) {
//                    String drawCardNum = "DrawCard" + Integer.toString(i);
//                    if (drawPileObj.has(drawCardNum)) {
//                        JSONObject drawCardObj = drawPileObj.getJSONObject(drawCardNum);
//                        retVal.drawPile.add(new Card());
//                        if (drawCardObj.has("id")) {
//                            retVal.drawPile.get(i).setId(drawCardObj.getInt("id"));
//                        }
//                        if (drawCardObj.has("cost")) {
//                            retVal.drawPile.get(i).setCost(drawCardObj.getInt("cost"));
//                        }
//                        if (drawCardObj.has("name")) {
//                            retVal.drawPile.get(i).setName(drawCardObj.getString("name"));
//                        }
//                        if (drawCardObj.has("description")) {
//                            retVal.drawPile.get(i).setDescription(drawCardObj.getString("description"));
//                        }
//                        if (drawCardObj.has("heartFlag")) {
//                            retVal.drawPile.get(i).setHeartFlag(drawCardObj.getInt("heartFlag"));
//                        }
//                        if (drawCardObj.has("heartDelta")) {
//                            retVal.drawPile.get(i).setHeartDelta(drawCardObj.getInt("heartDelta"));
//                        }
//                        if (drawCardObj.has("VPFlag")) {
//                            retVal.drawPile.get(i).setVPFlag(drawCardObj.getInt("VPFlag"));
//                        }
//                        if (drawCardObj.has("VPDelta")) {
//                            retVal.drawPile.get(i).setVPDelta(drawCardObj.getInt("VPDelta"));
//                        }
//                        if (drawCardObj.has("energyFlag")) {
//                            retVal.drawPile.get(i).setEnergyFlag(drawCardObj.getInt("energyFlag"));
//                        }
//                        if (drawCardObj.has("energyDelta")) {
//                            retVal.drawPile.get(i).setEnergyDelta(drawCardObj.getInt("energyDelta"));
//                        }
//                    }
//                }
//            }
//
//            if(obj.has("DisplayPile")) {
//                JSONObject displayPileObj = obj.getJSONObject("DisplayPile");
//                for (int i = 0; i < displayPileObj.length(); i++) {
//                    String displayCardNum = "DiscardCard" + Integer.toString(i);
//                    if (displayPileObj.has(displayCardNum)) {
//                        JSONObject displayCardObj = displayPileObj.getJSONObject(displayCardNum);
//                        if (displayCardObj.has("id")) {
//                            retVal.displayPile[i].setId(displayCardObj.getInt("id"));
//                        }
//                        if (displayCardObj.has("cost")) {
//                            retVal.displayPile[i].setCost(displayCardObj.getInt("cost"));
//                        }
//                        if (displayCardObj.has("name")) {
//                            retVal.displayPile[i].setName(displayCardObj.getString("name"));
//                        }
//                        if (displayCardObj.has("description")) {
//                            retVal.displayPile[i].setDescription(displayCardObj.getString("description"));
//                        }
//                        if (displayCardObj.has("heartFlag")) {
//                            retVal.displayPile[i].setHeartFlag(displayCardObj.getInt("heartFlag"));
//                        }
//                        if (displayCardObj.has("heartDelta")) {
//                            retVal.displayPile[i].setHeartDelta(displayCardObj.getInt("heartDelta"));
//                        }
//                        if (displayCardObj.has("VPFlag")) {
//                            retVal.displayPile[i].setVPFlag(displayCardObj.getInt("VPFlag"));
//                        }
//                        if (displayCardObj.has("VPDelta")) {
//                            retVal.displayPile[i].setVPDelta(displayCardObj.getInt("VPDelta"));
//                        }
//                        if (displayCardObj.has("energyFlag")) {
//                            retVal.displayPile[i].setEnergyFlag(displayCardObj.getInt("energyFlag"));
//                        }
//                        if (displayCardObj.has("energyDelta")) {
//                            retVal.displayPile[i].setEnergyDelta(displayCardObj.getInt("energyDelta"));
//                        }
//                    }
//                }
//            }
//
//            if(obj.has("DiscardPile")) {
//                JSONObject discardPileObj = obj.getJSONObject("DiscardPile");
//                for (int i = 0; i < discardPileObj.length(); i++) {
//                    String discardCardNum = "DiscardCard" + Integer.toString(i);
//                    if (discardPileObj.has(discardCardNum)) {
//                        JSONObject discardCardObj = discardPileObj.getJSONObject(discardCardNum);
//                        retVal.discardPile.add(new Card());
//                        if (discardCardObj.has("id")) {
//                            retVal.discardPile.get(i).setId(discardCardObj.getInt("id"));
//                        }
//                        if (discardCardObj.has("cost")) {
//                            retVal.discardPile.get(i).setCost(discardCardObj.getInt("cost"));
//                        }
//                        if (discardCardObj.has("name")) {
//                            retVal.discardPile.get(i).setName(discardCardObj.getString("name"));
//                        }
//                        if (discardCardObj.has("description")) {
//                            retVal.discardPile.get(i).setDescription(discardCardObj.getString("description"));
//                        }
//                        if (discardCardObj.has("heartFlag")) {
//                            retVal.discardPile.get(i).setHeartFlag(discardCardObj.getInt("heartFlag"));
//                        }
//                        if (discardCardObj.has("heartDelta")) {
//                            retVal.discardPile.get(i).setHeartDelta(discardCardObj.getInt("heartDelta"));
//                        }
//                        if (discardCardObj.has("VPFlag")) {
//                            retVal.discardPile.get(i).setVPFlag(discardCardObj.getInt("VPFlag"));
//                        }
//                        if (discardCardObj.has("VPDelta")) {
//                            retVal.discardPile.get(i).setVPDelta(discardCardObj.getInt("VPDelta"));
//                        }
//                        if (discardCardObj.has("energyFlag")) {
//                            retVal.discardPile.get(i).setEnergyFlag(discardCardObj.getInt("energyFlag"));
//                        }
//                        if (discardCardObj.has("energyDelta")) {
//                            retVal.discardPile.get(i).setEnergyDelta(discardCardObj.getInt("energyDelta"));
//                        }
//                    }
//                }
//            }

            if(obj.has("Players")) {
                JSONObject playersObj = obj.getJSONObject("Players");
                for (int i = 0; i < playersObj.length(); i++) {
                    String playerNum = "Player" + Integer.toString(i);
                    if (playersObj.has(playerNum)) {
                        JSONObject playerObj = playersObj.getJSONObject(playerNum);
                        if (playerObj.has("name") && playerObj.has("pid")) {
                            retVal.addPlayer(playerObj.getString("name"), playerObj.getString("pid"));
                        }
                        if (playerObj.has("heart")) {
                            retVal.players.get(i).setHealth(playerObj.getInt("heart"));
                        }
                        if (playerObj.has("vp")) {
                            retVal.players.get(i).setVictoryPoint(playerObj.getInt("vp"));
                        }
                        if (playerObj.has("energy")) {
                            retVal.players.get(i).setEnergy(playerObj.getInt("energy"));
                        }
                        if (playerObj.has("inTokyo")) {
                            retVal.players.get(i).setInTokyo(playerObj.getBoolean("inTokyo"));
                        }
                    }
                }
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return retVal;
    }
}