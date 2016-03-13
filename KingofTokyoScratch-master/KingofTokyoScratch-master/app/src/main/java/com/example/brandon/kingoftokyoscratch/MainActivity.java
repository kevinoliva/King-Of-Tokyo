package com.example.brandon.kingoftokyoscratch;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        OnInvitationReceivedListener, OnTurnBasedMatchUpdateReceivedListener,
        View.OnClickListener {

    private int rollCounter = 0; //number of times we have hit the roll button
    private int[] dice;
    private boolean[] keptDice;
    private TextView[] diceText;


    public static final String TAG = "MainActivity";

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Automatically start the sign-in flow when the Activity starts
    private boolean mAutoStartSignInFlow = true;

    // Current turn-based match
    private TurnBasedMatch mTurnBasedMatch;

    // Local convenience pointers
    public TextView mDataView;
    public TextView mTurnTextView;

    private AlertDialog mAlertDialog;

    // For our intents
    private static final int RC_SIGN_IN = 9001;
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_LOOK_AT_MATCHES = 10001;

    // How long to show toasts.
    final static int TOAST_DELAY = Toast.LENGTH_SHORT;

    // Should I be showing the turn API?
    public boolean isDoingTurn = false;
    private boolean gameOver;
    final static int VIC_GOAL = 10;

    // This is the current match we're in; null if not loaded
    public TurnBasedMatch mMatch;

    // This is the current match data after being unpersisted.
    // Do not retain references to match data once you have
    // taken an action on the match, such as takeTurn()
    public Turn mTurnData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the Google API Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // Setup signin and signout buttons
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.sign_in_button).setOnClickListener(this);

        mDataView = ((TextView) findViewById(R.id.data_view));
        mTurnTextView = ((TextView) findViewById(R.id.turn_counter_view));

        dice = new int[6];
        keptDice = new boolean[6];
        diceText = new TextView[6];
        for (int d = 0; d<6 ; d++){
            dice[d] = -1;
            keptDice[d] = false;
        }
        gameOver = false;
        diceText[0] = (TextView)findViewById(R.id.die0);
        diceText[1] = (TextView)findViewById(R.id.die1);
        diceText[2] = (TextView)findViewById(R.id.die2);
        diceText[3] = (TextView)findViewById(R.id.die3);
        diceText[4] = (TextView)findViewById(R.id.die4);
        diceText[5] = (TextView)findViewById(R.id.die5);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): Connecting to Google APIs");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): Disconnecting from Google APIs");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected(): Connection successful");

        // Retrieve the TurnBasedMatch from the connectionHint
        if (connectionHint != null) {
            mTurnBasedMatch = connectionHint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);

            if (mTurnBasedMatch != null) {
                if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
                    Log.d(TAG, "Warning: accessing TurnBasedMatch when not connected");
                }

                updateMatch(mTurnBasedMatch);
                return;
            }
        }

        setViewVisibility();

        // As a demonstration, we are registering this activity as a handler for
        // invitation and match events.

        // This is *NOT* required; if you do not register a handler for
        // invitation events, you will get standard notifications instead.
        // Standard notifications may be preferable behavior in many cases.
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);

        // Likewise, we are registering the optional MatchUpdateListener, which
        // will replace notifications you would get otherwise. You do *NOT* have
        // to register a MatchUpdateListener.
        Games.TurnBasedMultiplayer.registerMatchUpdateListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended():  Trying to reconnect.");
        mGoogleApiClient.connect();
        setViewVisibility();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(): attempting to resolve");
        if (mResolvingConnectionFailure) {
            // Already resolving
            Log.d(TAG, "onConnectionFailed(): ignoring connection failure, already resolving.");
            return;
        }

        // Launch the sign-in flow if the button was clicked or if auto sign-in is enabled
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;

            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult, RC_SIGN_IN,
                    getString(R.string.signin_other_error));
        }

        setViewVisibility();
    }

    // Displays your inbox. You will get back onActivityResult where
    // you will need to figure out what you clicked on.
    public void onCheckGamesClicked(View view) {
        Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(mGoogleApiClient);
        startActivityForResult(intent, RC_LOOK_AT_MATCHES);
    }

    // Open the create-game UI. You will get back an onActivityResult
    // and figure out what to do.
    public void onStartMatchClicked(View view) {
        Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(mGoogleApiClient,
                1, 3, false);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    // Create a one-on-one automatch game.
    public void onQuickMatchClicked(View view) {

        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                1, 1, 0);

        TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria).build();

        showSpinner();

        // Start the match
        ResultCallback<TurnBasedMultiplayer.InitiateMatchResult> cb = new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
            @Override
            public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                processResult(result);
            }
        };
        Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(cb);
    }

    // In-game controls

    // Cancel the game. Should possibly wait until the game is canceled before
    // giving up on the view.
    public void onCancelClicked(View view) {
        showSpinner();
        Games.TurnBasedMultiplayer.cancelMatch(mGoogleApiClient, mMatch.getMatchId())
                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.CancelMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.CancelMatchResult result) {
                        processResult(result);
                    }
                });
        isDoingTurn = false;
        setViewVisibility();
    }

    // Leave the game during your turn. Note that there is a separate
    // Games.TurnBasedMultiplayer.leaveMatch() if you want to leave NOT on your turn.
    public void onLeaveClicked(View view) {
        showSpinner();
        String nextParticipantId = getNextParticipantId();

        Games.TurnBasedMultiplayer.leaveMatchDuringTurn(mGoogleApiClient, mMatch.getMatchId(),
                nextParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.LeaveMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.LeaveMatchResult result) {
                        processResult(result);
                    }
                });
        setViewVisibility();
    }

    // Finish the game. Sometimes, this is your only choice.
    public void onFinishClicked(View view) {
        showSpinner();
        Games.TurnBasedMultiplayer.finishMatch(mGoogleApiClient, mMatch.getMatchId())
                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });

        Games.TurnBasedMultiplayer.dismissMatch(mGoogleApiClient, mMatch.getMatchId());
        isDoingTurn = false;
        gameOver = false;
        setViewVisibility();
    }


    // Upload your new gamestate, then take a turn, and pass it on to the next
    // player.
    public void onDoneClicked(View view) {
        showSpinner();

        String nextParticipantId = getNextParticipantId();
        // Create the next turn

        showSpinner();

        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(),
                mTurnData.persist(), nextParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });

        mTurnData = null;
    }

    // Sign-in, Sign out behavior

    // Update the visibility based on what state we're in.
    public void setViewVisibility() {
        boolean isSignedIn = (mGoogleApiClient != null) && (mGoogleApiClient.isConnected());

        if (!isSignedIn) {
            findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);

            if (mAlertDialog != null) {
                mAlertDialog.dismiss();
            }
            return;
        }

        ((TextView) findViewById(R.id.name_field)).setText(Games.Players.getCurrentPlayer(
                mGoogleApiClient).getDisplayName());
        findViewById(R.id.login_layout).setVisibility(View.GONE);

        if (isDoingTurn) {

            if(gameOver){
                //Toast.makeText(this, "num players = "+mTurnData.players.size(), TOAST_DELAY).show();
                findViewById(R.id.tokyo).setVisibility(View.GONE);
                findViewById(R.id.matchup_layout).setVisibility(View.GONE);
                findViewById(R.id.endGame).setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.gameOverString)).setText(R.string.gameOverString);
                findViewById(R.id.gameOverString).setVisibility(View.VISIBLE);
                Player temp = getCurrentPlayer();
                for(int i = 0; i < mTurnData.players.size();i++){
                    if(mTurnData.players.get(i).getVictoryPoint() >= temp.getVictoryPoint()){
                        temp = mTurnData.players.get(i);
                    }
                }
                ((TextView)findViewById(R.id.winnerName)).setText(temp.getName());
                Context context = getApplicationContext();
                ImageManager test = ImageManager.create(context);
                test.loadImage((ImageView) findViewById(R.id.winnerImage), mMatch.getParticipant(temp.getPid()).getHiResImageUri());
                findViewById(R.id.winnerImage).setVisibility(View.VISIBLE);
                findViewById(R.id.endGame).setVisibility(View.VISIBLE);
                findViewById(R.id.gameOverLayout).setVisibility(View.VISIBLE);
                return;
            }
            findViewById(R.id.gameOverLayout).setVisibility(View.GONE);
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.tokyo).setVisibility(View.VISIBLE);
            if(mTurnData.players.size() == 2){

                findViewById(R.id.p3Layout).setVisibility(View.GONE);
                findViewById(R.id.p4Layout).setVisibility(View.GONE);
            }
            else if(mTurnData.players.size() == 3){
                findViewById(R.id.p3Layout).setVisibility(View.VISIBLE);
                findViewById(R.id.p4Layout).setVisibility(View.GONE);
            }
            else if(mTurnData.players.size() == 4){
                findViewById(R.id.p3Layout).setVisibility(View.VISIBLE);
                findViewById(R.id.p4Layout).setVisibility(View.VISIBLE);
            }

            if(mTurnData.isTokyoAttacked){
                findViewById(R.id.diceLayouts).setVisibility(View.GONE);
                findViewById(R.id.tokyoAndRoll).setVisibility(View.GONE);
                findViewById(R.id.cardToDisplay).setVisibility(View.GONE);
                findViewById(R.id.decisionLayout).setVisibility(View.VISIBLE);
            }
            else{
                findViewById(R.id.decisionLayout).setVisibility(View.GONE);
                findViewById(R.id.diceLayouts).setVisibility(View.VISIBLE);
                findViewById(R.id.tokyoAndRoll).setVisibility(View.VISIBLE);
                findViewById(R.id.cardToDisplay).setVisibility(View.VISIBLE);
            }

            updateStats();
//            findViewById(R.id.gameplay_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.matchup_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.tokyo).setVisibility(View.GONE);
            findViewById(R.id.gameOverLayout).setVisibility(View.GONE);
//            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);
        }
    }

    // Switch to gameplay view.
    public void setGameplayUI() {
        isDoingTurn = true;
        setViewVisibility();
        /*mDataView.setText(mTurnData.data);
        mTurnTextView.setText("Turn " + mTurnData.turnCounter);*/

        //set image uri
        findViewById(R.id.tokyoImage).setVisibility(View.INVISIBLE);
        findViewById(R.id.currentTokyo).setVisibility(View.INVISIBLE);
        Context context = getApplicationContext();
        ImageManager test = ImageManager.create(context);


        for(int i = 0; i < mTurnData.players.size(); i++){
            if(mTurnData.players.get(i).getInTokyo()){
               TextView tempMidPlayer = (TextView)findViewById(R.id.tokyoPlayerName);
                tempMidPlayer.setText(mTurnData.players.get(i).getName());

                String tempPID = mTurnData.players.get(i).getPid();
                test.loadImage((ImageView)findViewById(R.id.tokyoImage), mMatch.getParticipant(tempPID).getHiResImageUri());
                findViewById(R.id.tokyoImage).setVisibility(View.VISIBLE);
                findViewById(R.id.currentTokyo).setVisibility(View.VISIBLE);
            }



        }
    }

    // Helpful dialogs

    public void showSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.VISIBLE);
    }

    public void dismissSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.GONE);
    }

    // Generic warning/info dialog
    public void showWarning(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                    }
                });

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }

    // Rematch dialog
    public void askForRematch() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setMessage("Do you want a rematch?");

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Sure, rematch!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                rematch();
                            }
                        })
                .setNegativeButton("No.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });

        alertDialogBuilder.show();
    }

    // This function is what gets called when you return from either the Play
    // Games built-in inbox, or else the create game built-in interface.
    @Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        if (request == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (response == Activity.RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, request, response, R.string.signin_other_error);
            }
        } else if (request == RC_LOOK_AT_MATCHES) {
            // Returning from the 'Select Match' dialog

            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            TurnBasedMatch match = data
                    .getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);

            if (match != null) {
                updateMatch(match);
            }

            Log.d(TAG, "Match = " + match);
        } else if (request == RC_SELECT_PLAYERS) {
            // Returned from 'Select players to Invite' dialog

            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            // get the invitee list
            final ArrayList<String> invitees = data
                    .getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

            // get automatch criteria
            Bundle autoMatchCriteria;

            int minAutoMatchPlayers = data.getIntExtra(
                    Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(
                    Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            } else {
                autoMatchCriteria = null;
            }

            TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria).build();

            // Start the match
            Games.TurnBasedMultiplayer.createMatch(mGoogleApiClient, tbmc).setResultCallback(
                    new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                            processResult(result);
                        }
                    });
            showSpinner();
        }
    }

    // startMatch() happens in response to the createTurnBasedMatch()
    // above. This is only called on success, so we should have a
    // valid match object. We're taking this opportunity to setup the
    // game, saving our initial state. Calling takeTurn() will
    // callback to OnTurnBasedMatchUpdated(), which will show the game
    // UI.
    public void startMatch(TurnBasedMatch match) {
        mTurnData = new Turn();
        // Some basic turn data

        mMatch = match;

        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = mMatch.getParticipantId(playerId);

        for(Participant p : mMatch.getParticipants()){
            mTurnData.addPlayer(p.getDisplayName(), p.getParticipantId());
        }

        showSpinner();

        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, match.getMatchId(),
                mTurnData.persist(), myParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
    }

    // If you choose to rematch, then call it and wait for a response.
    public void rematch() {
        showSpinner();
        Games.TurnBasedMultiplayer.rematch(mGoogleApiClient, mMatch.getMatchId()).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                        processResult(result);
                    }
                });
        mMatch = null;
        isDoingTurn = false;
    }

    /**
     * Get the next participant. In this function, we assume that we are
     * round-robin, with all known players going before all automatch players.
     * This is not a requirement; players can go in any order. However, you can
     * take turns in any order.
     *
     * @return participantId of next player, or null if automatching
     */
    public String getNextParticipantId() {
        String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
        String myParticipantId = mMatch.getParticipantId(playerId);

        ArrayList<String> participantIds = mMatch.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }

        if (mMatch.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we start over.
            return participantIds.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }

    // This is the main function that gets called when players choose a match
    // from the inbox, or else create a match and want to start it.
    public void updateMatch(TurnBasedMatch match) {
        mMatch = match;

        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                showWarning("Canceled!", "This game was canceled!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                showWarning("Expired!", "This game is expired.  So sad!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an automatch partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                gameOver = true;
//                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
//                    showWarning(
//                            "Complete!",
//                            "This game is over; someone finished it, and so did you!  There is nothing to be done.");
//                    break;
//                }
//
//                // Note that in this state, you must still call "Finish" yourself,
//                // so we allow this to continue.
//                showWarning("Complete!",
//                        "This game is over; someone finished it!  You can only finish it now.");
        }

        // OK, it's active. Check on turn status.
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                mTurnData = Turn.unpersist(mMatch.getData());
                if(gameOver) {
                    isDoingTurn = true;
                    setViewVisibility();
                    return;
                }
                rollCounter = 0; //reset roll counter
                ((Button)findViewById(R.id.buttonRoll)).setText(R.string.firstRoll); //reset button text
                Player curPlayer = getCurrentPlayer();
                if(mTurnData.drawPile.size() == 0 && mTurnData.discardPile.size() == 0){
                    //mTurnData.setUpCards();
                    //updateCardText();
                }
                updateCardText();

                if(curPlayer.getInTokyo()) { //current player is tokyo player
                    if (!mTurnData.isTokyoAttacked()) { //starting a regular turn in tokyo
                        curPlayer.updateVictoryPoint(2); //2 points if still in Tokyo
                        Toast.makeText(this, "+2 VP for remaining in Tokyo", TOAST_DELAY).show();
                        Games.Achievements.unlock(mGoogleApiClient, "CgkImsu-1fcFEAIQDQ");
                    }
                }
                else { //not in tokyo
                    //if returning from special turn
                    if (mTurnData.isTokyoAttacked() && mTurnData.getLastAttackerId().equals(getCurrentPlayer().getPid())) {
                        if(mTurnData.isTokyoEmpty()){
                            getCurrentPlayer().setInTokyo(true); //last player left tokyo if empty
                            getCurrentPlayer().updateVictoryPoint(1); //Get 1 victory point for taking tokyo.
                            Toast.makeText(this, "You have taken Tokyo. +1 VP.", TOAST_DELAY).show();
                        }
                        mTurnData.setTokyoAttacked(false);
                        rollCounter = 4;
                        ((Button)findViewById(R.id.buttonRoll)).setText(R.string.finishTurn);
                        turnCardsClickable();
                    }
                }
                setGameplayUI();
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                // Should return results.
                showWarning("Alas...", "It's not your turn.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWarning("Good inititative!",
                        "Still waiting for invitations.\n\nBe patient!");
        }

        mTurnData = null;

        setViewVisibility();
    }

    private void processResult(TurnBasedMultiplayer.CancelMatchResult result) {
        dismissSpinner();

        if (!checkStatusCode(null, result.getStatus().getStatusCode())) {
            return;
        }

        isDoingTurn = false;

        showWarning("Match",
                "This match is canceled.  All other players will have their game ended.");
    }

    private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();

        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }

        if (match.getData() != null) {
            // This is a game that has already started, so I'll just start
            updateMatch(match);
            return;
        }

        startMatch(match);
    }


    private void processResult(TurnBasedMultiplayer.LeaveMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
        showWarning("Left", "You've left this match.");
    }


    public void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        if (match.canRematch()) {
            askForRematch();
        }

        isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);

        if (isDoingTurn) {
            updateMatch(match);
            return;
        }

        setViewVisibility();
    }

    // Handle notification events.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        Toast.makeText(
                this,
                "An invitation has arrived from "
                        + invitation.getInviter().getDisplayName(), TOAST_DELAY)
                .show();
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        Toast.makeText(this, "An invitation was removed.", TOAST_DELAY).show();
    }

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
        Toast.makeText(this, "A match was updated.", TOAST_DELAY).show();
    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        Toast.makeText(this, "A match was removed.", TOAST_DELAY).show();

    }

    public void showErrorMessage(TurnBasedMatch match, int statusCode,
                                 int stringId) {

        showWarning("Warning", getResources().getString(stringId));
    }

    // Returns false if something went wrong, probably. This should handle
    // more cases, and probably report more accurate results.
    private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
        switch (statusCode) {
            case GamesStatusCodes.STATUS_OK:
                return true;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
                // This is OK; the action is stored by Google Play Services and will
                // be dealt with later.
                Toast.makeText(
                        this,
                        "Stored action for later.  (Please remove this toast before release.)",
                        TOAST_DELAY).show();
                // NOTE: This toast is for informative reasons only; please remove
                // it from your final application.
                return true;
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                showErrorMessage(match, statusCode,
                        R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_already_rematched);
                break;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
                showErrorMessage(match, statusCode,
                        R.string.network_error_operation_failed);
                break;
            case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
                showErrorMessage(match, statusCode,
                        R.string.client_reconnect_required);
                break;
            case GamesStatusCodes.STATUS_INTERNAL_ERROR:
                showErrorMessage(match, statusCode, R.string.internal_error);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
                showErrorMessage(match, statusCode,
                        R.string.match_error_inactive_match);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_locally_modified);
                break;
            default:
                showErrorMessage(match, statusCode, R.string.unexpected_status);
                Log.d(TAG, "Did not have warning or string to deal with: "
                        + statusCode);
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                // Check to see the developer who's running this sample code read the instructions :-)
                // NOTE: this check is here only because this is a sample! Don't include this
                // check in your actual production app.
                if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
                    Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
                }

                mSignInClicked = true;
                mTurnBasedMatch = null;
                findViewById(R.id.sign_in_button).setVisibility(View.GONE);
                mGoogleApiClient.connect();
                break;
            case R.id.sign_out_button:
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
                setViewVisibility();


                break;
        }
    }

    //Below this was not modified from SkeletonTBMP and is original code

    public void rollDice() {
        for (int i = 0; i < 6; i++) {
            if (!keptDice[i]) {
                Random rn = new Random();
                int r = rn.nextInt(6);
                String image = "";
                dice[i] = r;

                switch (r) {

                    case 0:
                        image = "Energy";
                        break;
                    case 1:
                        image = "1";
                        break;
                    case 2:
                        image = "2";
                        break;
                    case 3:
                        image = "3";
                        break;
                    case 4:
                        image = "Claw";
                        break;
                    case 5:
                        image = "Heart";
                        break;

                }
                diceText[i].setText(image);
            }
        }

    }

    public void generalButtonClicked(View view){
        switch (rollCounter){
            case 0:
                rollDice();
                ((Button)findViewById(R.id.buttonRoll)).setText(R.string.secondRoll);
                break;
            case 1:
                rollDice();
                ((Button)findViewById(R.id.buttonRoll)).setText(R.string.thirdRoll);
                break;
            case 2:
                rollDice();
                resolveDice();
                updateStats();
                //resetKeptDice();
                keepAllDice();
                if(mTurnData.isTokyoAttacked()){
                    ((Button)findViewById(R.id.buttonRoll)).setText(R.string.askTokyo);
                }
                else{
                    ((Button)findViewById(R.id.buttonRoll)).setText(R.string.finishTurn);
                    rollCounter++;//skip case 3 and go to next one
                    turnCardsClickable();
                }
                break;
            case 3:
                sendTokyoRequest();
                ((Button)findViewById(R.id.buttonRoll)).setText(R.string.finishTurn);
                break;
            case 4:
                turnCardsUnclickable();
                resetKeptDice();
                for (int i = 0; i< 6; i++){
                    diceText[i].setText("");
                }
                if(getCurrentPlayer().getVictoryPoint() >= VIC_GOAL){
                    gameOver = true;
                    Games.Achievements.unlock(mGoogleApiClient, "CgkImsu-1fcFEAIQDw");
                    setViewVisibility();
                    return;
                }

                onDoneClicked(view);
                break;
        }
        rollCounter++;
    }

    public void resolveDice(){
        int numHearts = 0;
        int numEnergy = 0;
        int numClaws = 0;
        int numOf1 = 0;
        int numOf2 = 0;
        int numOf3 = 0;
        int vp = 0;

        for (int i = 0; i < 6; i++){
            switch (dice[i]) {
                case 0: numEnergy++;
                    break;
                case 1:  numOf1++;
                    break;
                case 2:  numOf2++;
                    break;
                case 3:  numOf3++;
                    break;
                case 4:  numClaws++;
                    break;
                case 5:  numHearts++;
                    break;
                default:
                    break;
            }
        }

        if(numOf3 >= 6){
            Games.Achievements.unlock(mGoogleApiClient, "CgkImsu-1fcFEAIQDg");
        }
        if(numHearts >= 6){
            Games.Achievements.unlock(mGoogleApiClient, "CgkImsu-1fcFEAIQEQ");
        }
        if(numEnergy >= 6){
            Games.Achievements.unlock(mGoogleApiClient, "CgkImsu-1fcFEAIQEA");
        }

        if(numOf1 >= 3){
            numOf1 -= 3;
            vp = vp + 1 + numOf1;
        }
        if(numOf2 >= 3){
            numOf2 -= 3;
            vp = vp + 2 + numOf2;
        }
        if(numOf3 >= 3){
            numOf3 -= 3;
            vp = vp + 3 + numOf3;
        }

        int curP = getCurP();
        mTurnData.players.get(curP).updateVictoryPoint(vp);
        mTurnData.players.get(curP).updateEnergy(numEnergy);
        mTurnData.players.get(curP).updateHealth(numHearts);

        //attack another player or take tokyo
        if(numClaws > 0){
            //if(!tokyoFull){ //tokyo is empty, take tokyo as yours
            if(mTurnData.isTokyoEmpty()){ //tokyo is empty, take tokyo as yours
                mTurnData.players.get(curP).setInTokyo(true);
                mTurnData.players.get(curP).updateVictoryPoint(1);
                Toast.makeText(this, "You have taken Tokyo!", TOAST_DELAY).show();
            }
            else if(getCurrentPlayer().getInTokyo()){//current player is in tokyo
                for(Player p : mTurnData.players){
                    if(!p.getInTokyo()){
                        p.takeDamage(numClaws);
                    }
                }
            }
            else { //current player not in tokyo and tokyo not empty
                Player tokyoPlayer = getTokyoPlayer();
                tokyoPlayer.takeDamage(numClaws);
                if(tokyoPlayer.getHealth() == 0){ //tokyo player dies, take tokyo
                    tokyoPlayer.setInTokyo(false);
                    getCurrentPlayer().setInTokyo(true);
                    Toast.makeText(this, "You have taken Tokyo. +1 VP.", TOAST_DELAY).show();
                }
                else { //tokyo player still alive
                    mTurnData.setTokyoAttacked(true);
                    mTurnData.setLastAttackerId(mTurnData.players.get(curP).getPid());
                }
            }

        }
    }

    public void updateStats(){
        Player tempPlayer = mTurnData.players.get(0);
        ((TextView)findViewById(R.id.name0)).setText(shortenName(tempPlayer.getName()));
        ((TextView)findViewById(R.id.heart0)).setText(Integer.toString(tempPlayer.getHealth()));
        ((TextView)findViewById(R.id.vp0)).setText(Integer.toString(tempPlayer.getVictoryPoint()));
        ((TextView)findViewById(R.id.energy0)).setText(Integer.toString(tempPlayer.getEnergy()));

        tempPlayer = mTurnData.players.get(1);
        ((TextView)findViewById(R.id.name1)).setText(shortenName(tempPlayer.getName()));
        ((TextView)findViewById(R.id.heart1)).setText(Integer.toString(tempPlayer.getHealth()));
        ((TextView)findViewById(R.id.vp1)).setText(Integer.toString(tempPlayer.getVictoryPoint()));
        ((TextView)findViewById(R.id.energy1)).setText(Integer.toString(tempPlayer.getEnergy()));

        if(mTurnData.players.size() > 2) {
            tempPlayer = mTurnData.players.get(2);
            ((TextView)findViewById(R.id.name2)).setText(shortenName(tempPlayer.getName()));
            ((TextView) findViewById(R.id.heart2)).setText(Integer.toString(tempPlayer.getHealth()));
            ((TextView) findViewById(R.id.vp2)).setText(Integer.toString(tempPlayer.getVictoryPoint()));
            ((TextView) findViewById(R.id.energy2)).setText(Integer.toString(tempPlayer.getEnergy()));
        }
        if(mTurnData.players.size() > 3) {
            tempPlayer = mTurnData.players.get(3);
            ((TextView)findViewById(R.id.name3)).setText(shortenName(tempPlayer.getName()));
            ((TextView) findViewById(R.id.heart3)).setText(Integer.toString(tempPlayer.getHealth()));
            ((TextView) findViewById(R.id.vp3)).setText(Integer.toString(tempPlayer.getVictoryPoint()));
            ((TextView) findViewById(R.id.energy3)).setText(Integer.toString(tempPlayer.getEnergy()));
        }
    }

    public void updateCardText(){
//        Card tmpCard = mTurnData.displayPile[0];
//        ((TextView)findViewById(R.id.card0)).setText("Cost: "+tmpCard.getCost()+"\n"+tmpCard.getDescription());
//        tmpCard = mTurnData.displayPile[1];
//        ((TextView)findViewById(R.id.card1)).setText("Cost: "+tmpCard.getCost()+"\n"+tmpCard.getDescription());
//        tmpCard = mTurnData.displayPile[2];
//        ((TextView)findViewById(R.id.card2)).setText("Cost: " + tmpCard.getCost() + "\n" + tmpCard.getDescription());

        ((TextView)findViewById(R.id.card0)).setText("Cost: 3\n+1 VP\nor\n+2 VP");
        ((TextView)findViewById(R.id.card1)).setText("Cost: 5\n+3 VP\nor\n+4 VP");
        ((TextView)findViewById(R.id.card2)).setText("Cost: 7\n+5 VP\nor\n+6 VP");
    }

    public void keepDie0(View view){
        keepDie(0);
    }

    public void keepDie1(View view){
        keepDie(1);
    }

    public void keepDie2(View view){
        keepDie(2);
    }

    public void keepDie3(View view){
        keepDie(3);
    }

    public void keepDie4(View view){
        keepDie(4);
    }

    public void keepDie5(View view){
        keepDie(5);
    }

    public void keepDie(int d){
        if (rollCounter > 0 && rollCounter < 3) {
            if (keptDice[d]) {
                keptDice[d] = false;
                diceText[d].setBackgroundColor(Color.WHITE);
            }
            else {
                keptDice[d] = true;
                diceText[d].setBackgroundColor(Color.YELLOW);
            }
        }
        else if(rollCounter == 3){
            diceText[d].setBackgroundColor(Color.WHITE);
        }
    }

    public void resetKeptDice(){
        for (int i = 0; i< 6; i++){
            keptDice[i] = false;
            diceText[i].setBackgroundColor(Color.WHITE);
        }
    }

    public void keepAllDice(){
        for (int i = 0; i< 6; i++){
            keptDice[i] = true;
            diceText[i].setBackgroundColor(Color.YELLOW);
        }
    }

    public String shortenName(String name){

        int spaceIndex = name.indexOf(" ");
        if(spaceIndex < 0){
            if(name.length() < 7){ //no space in short name
                return name;
            }

            return name.substring(0,7); //no space in long name
        }
        else {
            if (spaceIndex < 7) {
                return name.substring(0, spaceIndex); //short name with space
            }
            return name.substring(0, 7); //long name with space
        }
    }

    public int getCurP(){
        int curP = -1;
        for (int i = 0; i < mTurnData.players.size(); i++){
            String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
            String myParticipantId = mMatch.getParticipantId(playerId);
            if(mTurnData.players.get(i).getPid().equals(myParticipantId)){
                curP = i;
            }
        }
        return curP;
    }

    public Player getCurrentPlayer(){
        for (int i = 0; i < mTurnData.players.size(); i++){
            String playerId = Games.Players.getCurrentPlayerId(mGoogleApiClient);
            String myParticipantId = mMatch.getParticipantId(playerId);
            if(mTurnData.players.get(i).getPid().equals(myParticipantId)){
                return mTurnData.players.get(i);
            }
        }
        return null;
    }

    public Player getTokyoPlayer(){
        for (int i = 0; i < mTurnData.players.size(); i++){
            if (mTurnData.players.get(i).getInTokyo()){
                return mTurnData.players.get(i);
            }
        }
        return null;
    }



    public void sendTokyoRequest() {
        showSpinner();

        //String nextParticipantId = getNextParticipantId();
        String tokyoParticipantId = getTokyoPlayer().getPid();
        // Create the next turn

        showSpinner();

        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(),
                mTurnData.persist(), tokyoParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });

        mTurnData = null;
    }

    public void staying(View view){
        showSpinner();

        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(),
                mTurnData.persist(), mTurnData.getLastAttackerId()).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });

        mTurnData = null;

    }
    public void leaving(View view){
        getCurrentPlayer().setInTokyo(false);
        showSpinner();

        Games.TurnBasedMultiplayer.takeTurn(mGoogleApiClient, mMatch.getMatchId(),
                mTurnData.persist(), mTurnData.getLastAttackerId()).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });

        mTurnData = null;
    }

    public void buyCard0(View view){
        int cost = 3;
        Player curPlayer = getCurrentPlayer();
        if(curPlayer.getEnergy() >= cost) {
            //take cost from player's energy
            curPlayer.updateEnergy(-1*cost);

            //pick random number between 1 and 2
            Random rn = new Random();
            int randomVP = rn.nextInt(2) + 1;

            //update victory points
            curPlayer.updateVictoryPoint(randomVP);

            Toast.makeText(this, "+"+randomVP+" VP", TOAST_DELAY).show();
        }
        else { //too poor to buy card
            Toast.makeText(this, "You don't have enough Energy to buy this card.", TOAST_DELAY).show();
        }

        updateStats();

        //buyCard(0);
    }

    public void buyCard1(View view){
        int cost = 5;
        Player curPlayer = getCurrentPlayer();
        if(curPlayer.getEnergy() >= cost) {
            //take cost from player's energy
            curPlayer.updateEnergy(-1*cost);

            //pick random number between 3 and 4
            Random rn = new Random();
            int randomVP = rn.nextInt(2) + 3;

            //update victory points
            curPlayer.updateVictoryPoint(randomVP);

            Toast.makeText(this, "+"+randomVP+" VP", TOAST_DELAY).show();
        }
        else { //too poor to buy card
            Toast.makeText(this, "You don't have enough Energy to buy this card.", TOAST_DELAY).show();
        }

        updateStats();

        //buyCard(1);
    }

    public void buyCard2(View view){
        int cost = 7;
        Player curPlayer = getCurrentPlayer();
        if(curPlayer.getEnergy() >= cost) {
            //take cost from player's energy
            curPlayer.updateEnergy(-1*cost);

            //pick random number between 5 and 6
            Random rn = new Random();
            int randomVP = rn.nextInt(2) + 5;

            //update victory points
            curPlayer.updateVictoryPoint(randomVP);

            Toast.makeText(this, "+"+randomVP+" VP", TOAST_DELAY).show();
        }
        else { //too poor to buy card
            Toast.makeText(this, "You don't have enough Energy to buy this card.", TOAST_DELAY).show();
        }

        updateStats();

        //buyCard(2);
    }

    public void buyCard(int num){
        Player curPlayer = getCurrentPlayer();
        Card curCard = mTurnData.displayPile[num];
        if(curPlayer.getEnergy() >= curCard.getCost()) {

            //take cost from player's energy
            curPlayer.updateEnergy(-1 * curCard.getCost());

            //update health if applicable
            switch (curCard.getHeartFlag()) {
                case 0:
                    break;
                case 1:
                    curPlayer.updateHealth(curCard.getHeartDelta());
                    break;
                case 2:
                    for (Player p : mTurnData.players) {
                        if (!p.getPid().equals(curPlayer.getPid())) {
                            p.updateHealth(curCard.getHeartDelta());
                        }
                    }
                    break;
                case 3:
                    for (Player p : mTurnData.players) {
                        p.updateHealth(curCard.getHeartDelta());
                    }
                    break;
            }

            //update victory points if applicable
            switch (curCard.getVPFlag()) {
                case 0:
                    break;
                case 1:
                    curPlayer.updateVictoryPoint(curCard.getVPDelta());
                    break;
                case 2:
                    for (Player p : mTurnData.players) {
                        if (!p.getPid().equals(curPlayer.getPid())) {
                            p.updateVictoryPoint(curCard.getVPDelta());
                        }
                    }
                    break;
                case 3:
                    for (Player p : mTurnData.players) {
                        p.updateVictoryPoint(curCard.getVPDelta());
                    }
                    break;
            }

            //update energy if applicable
            switch (curCard.getEnergyFlag()) {
                case 0:
                    break;
                case 1:
                    curPlayer.updateEnergy(curCard.getEnergyDelta());
                    break;
                case 2:
                    for (Player p : mTurnData.players) {
                        if (!p.getPid().equals(curPlayer.getPid())) {
                            p.updateEnergy(curCard.getEnergyDelta());
                        }
                    }
                    break;
                case 3:
                    for (Player p : mTurnData.players) {
                        p.updateEnergy(curCard.getEnergyDelta());
                    }
                    break;
            }

            Toast.makeText(this, curCard.getDescription(), TOAST_DELAY).show();

            //discards bought card and places new one out
            mTurnData.replaceCard(num);

            updateStats();
            updateCardText();
        }
        else { //too poor to buy card
            Toast.makeText(this, "You don't have enough Energy to buy this card.", TOAST_DELAY).show();
        }
    }

    public void wipeCards(View view){
        mTurnData.replaceCard(0);
        mTurnData.replaceCard(1);
        mTurnData.replaceCard(2);
    }

    public void turnCardsClickable(){
        findViewById(R.id.card0).setClickable(true);
        findViewById(R.id.card1).setClickable(true);
        findViewById(R.id.card2).setClickable(true);
    }

    public void turnCardsUnclickable(){
        findViewById(R.id.card0).setClickable(false);
        findViewById(R.id.card1).setClickable(false);
        findViewById(R.id.card2).setClickable(false);
    }
}