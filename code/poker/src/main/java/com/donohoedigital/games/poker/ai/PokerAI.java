/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2025 Doug Donohoe
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * 
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images, 
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials) 
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives 
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets 
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 * 
 * For inquiries regarding commercial licensing of this source code or 
 * the use of names, logos, images, text, or other assets, please contact 
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.ai;

import com.ddpoker.holdem.PlayerAction;
import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.base.ErrorCodes;
import com.donohoedigital.comms.DMTypedHashMap;
import com.donohoedigital.comms.DataCoder;
import com.donohoedigital.comms.MsgState;
import com.donohoedigital.comms.TokenizedList;
import com.donohoedigital.config.ConfigUtils;
import com.donohoedigital.config.Perf;
import com.donohoedigital.games.config.GameState;
import com.donohoedigital.games.engine.EngineGameAI;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.engine.PokerSaveDetails;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.event.PokerTableListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

// TODO: provide APIs for logging that don't require DDLogger
// TODO: provide generic idiot-proof marshal/demarshal interface

@DataCoder('%')
public class PokerAI extends EngineGameAI implements PokerTableListener, PropertyChangeListener
{
    static Logger logger = LogManager.getLogger(PokerAI.class);

    private OpponentModel opponentModel_ = new OpponentModel();

    protected PlayerType playerType_;

    private static final String MAP_ACTED_THIS_ROUND = "actedThisRound";

    public static final int POSITION_NONE = -1;
    public static final int POSITION_EARLY = 0;
    public static final int POSITION_MIDDLE = 1;
    public static final int POSITION_LATE = 2;
    public static final int POSITION_BUTTON = 3;
    public static final int POSITION_LAST = 3;
    public static final int POSITION_SMALL = 4;
    public static final int POSITION_BIG = 5;

    public static final int PRE_FLOP = HoldemHand.ROUND_PRE_FLOP;
    public static final int FLOP = HoldemHand.ROUND_FLOP;
    public static final int TURN = HoldemHand.ROUND_TURN;
    public static final int RIVER = HoldemHand.ROUND_RIVER;

    private DMTypedHashMap map_;

    /**
     * Creates a new instance of PokerAI
     */
    public PokerAI()
    {
        super(false);
        if (false) Perf.construct(this, null);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        String name = evt.getPropertyName();

        if (name.equals(PokerGame.PROP_GAME_OVER))
        {
            gameOver();
        }
    }

    public void gameLoaded()
    {
        addListeners();
    }

    protected void gameOver()
    {

    }

    public void init()
    {
        map_ = new DMTypedHashMap();

        opponentModel_.init();
    }

    public boolean hasActedThisRound()
    {
        return map_.getBoolean(MAP_ACTED_THIS_ROUND, false);
    }

    public void setPlayerType(PlayerType playerType)
    {
        playerType_ = playerType;
    }

    public PlayerType getPlayerType()
    {
        return playerType_;
    }

    /**
     * Set player we are AI for
     */
    public void setPokerPlayer(PokerPlayer player)
    {
        PokerPlayer old = getPokerPlayer();

        if (player != old)
        {
            if (old != null)
            {
                removeListeners();
            }
            if (player != null)
            {
                setGamePlayer(player);
                addListeners();
            }
        }
    }

    public void addListeners()
    {
        PokerPlayer player = getPokerPlayer();

        if (player != null)
        {
            PokerTable table = player.getTable();

            if (table != null)
            {
                // logger.info("Adding listeners for " + player.getName());

                table.addPokerTableListener(this, PokerTableEvent.TYPES_ALL);

                PokerGame game = table.getGame();

                game.addPropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
                game.addPropertyChangeListener(PokerGame.PROP_GAME_OVER, this);
            }
        }
    }

    private void removeListeners()
    {
        PokerPlayer player = getPokerPlayer();

        if (player != null)
        {
            PokerTable table = player.getTable();

            if (table != null)
            {
                // logger.info("Removing listeners for " + player.getName());

                table.removePokerTableListener(this, PokerTableEvent.TYPES_ALL);

                PokerGame game = table.getGame();

                game.removePropertyChangeListener(PokerGame.PROP_GAME_LOADED, this);
                game.removePropertyChangeListener(PokerGame.PROP_GAME_OVER, this);
            }
        }
    }

    /**
     * Get player in a particular seat
     */
    public PokerPlayer getPokerPlayer(int seat)
    {
        return getPokerPlayer().getTable().getPlayer(seat);
    }

    /**
     * Get player we are AI for
     */
    public PokerPlayer getPokerPlayer()
    {
        return (PokerPlayer)getGamePlayer();
    }

    public static PokerAI createPokerAI(PlayerType playerType)
    {
        String className = playerType.getAIClassName();

        try
        {
            Class c = Class.forName(className);

            PokerAI ai = (PokerAI) ConfigUtils.newInstance(c);

            ai.setPlayerType(playerType);

            ai.init();

            return ai;
        }
        catch (ClassNotFoundException e)
        {
            throw new ApplicationError("Can't find " + className);
        }
    }

    /**
     * Return action AI would take.  This can be called for a human player
     * to get a "hint" if desired.
     */
    public HandAction getHandAction(boolean bQuick)
    {
        PlayerAction action = getAction(bQuick);

        PokerPlayer player = getPokerPlayer();

        HoldemHand hand = player.getHoldemHand();

        int call = hand.getCall(player);
        int round = hand.getRound();
        int playerchips = player.getChipCount();
        int minraise = hand.getMinRaise();

        // translate new-style action to old-style action

        if ((action.isCheck() || action.isFold() || action.isCall()) && (call == 0))
        {
            return new HandAction(player, round, HandAction.ACTION_CHECK, action.getReason());
        }
        else if (action.isFold())
        {
            return new HandAction(player, round, HandAction.ACTION_FOLD, action.getReason());
        }
        else if (action.isCall())
        {
            if (call <= playerchips)
            {
                return new HandAction(player, round, HandAction.ACTION_CALL, call, action.getReason());
            }
            else
            {
                return new HandAction(player, round, HandAction.ACTION_CALL, playerchips, action.getReason());
            }

        }
        else if (action.isBet() || action.isRaise())
        {
            int amount = getBetAmount();

            if (amount == 0)
            {
                amount = call + minraise;
            }

            if (amount > playerchips)
            {
                amount = playerchips;
            }
            else if ((amount - call < minraise) && (amount < playerchips))
            {
                if (playerchips - call < minraise)
                {
                    amount = playerchips;
                }
                else
                {
                    amount = call + minraise;
                }
            }

            if ((call == 0) && (round != HoldemHand.ROUND_PRE_FLOP))
            {
                return new HandAction(player, round, HandAction.ACTION_BET, amount, action.getReason());
            }
            else if (amount <= call)
            {
                return new HandAction(player, round, HandAction.ACTION_CALL, amount, action.getReason());
            }
            else
            {
                return new HandAction(player, round, HandAction.ACTION_RAISE, amount - call, action.getReason());
            }
        }
        else
        {
            return null;
        }
    }

    public boolean isRebuy()
    {
        return wantsRebuy();
    }

    public boolean isAddon()
    {
        return wantsAddon();
    }

    /**
     * This event listener implementation dispatches events to comprehensible
     * public APIs, with some events being ignored or handled internally.
     */
    public void tableEventOccurred(PokerTableEvent event)
    {
        boolean hasPocket = (getPocket() != null) && (event.getTable().getHoldemHand() != null);

        // do items required in the calling thread
        switch (event.getType())
        {
            case PokerTableEvent.TYPE_PLAYER_ADDED:

                playerAdded(event.getSeat());
                break;

            case PokerTableEvent.TYPE_PLAYER_REMOVED:

                playerRemoved(event.getSeat());
                break;

            case PokerTableEvent.TYPE_NEW_HAND:

                newHand();
                break;

            case PokerTableEvent.TYPE_DEALER_ACTION:

                map_.setBoolean(MAP_ACTED_THIS_ROUND, Boolean.FALSE);

                if (hasPocket) switch (event.getRound())
                {
                    case HoldemHand.ROUND_PRE_FLOP:
                        dealtPockets();
                        break;
                    case HoldemHand.ROUND_FLOP:
                        dealtFlop();
                        break;
                    case HoldemHand.ROUND_TURN:
                        dealtTurn();
                        break;
                    case HoldemHand.ROUND_RIVER:
                        dealtRiver();
                        break;
                }
                break;

            case PokerTableEvent.TYPE_LEVEL_CHANGED:

                blindsChanged();
                break;

            case PokerTableEvent.TYPE_END_HAND:

                if (hasPocket) endHand();
                break;

            case PokerTableEvent.TYPE_PLAYER_ACTION:

                if (!hasPocket) break;

                int seat = event.getPlayer().getSeat();

                if (seat == getPokerPlayer().getSeat())
                {
                    map_.setBoolean(MAP_ACTED_THIS_ROUND, Boolean.TRUE);
                }

                HandAction action = event.getAction();

                int actionType = HandAction.ACTION_NONE;

                // TODO: notify on blind / ante / overbet events?
                switch (action.getAction())
                {
                    case HandAction.ACTION_FOLD:
                        actionType = PlayerAction.FOLD;
                        break;
                    case HandAction.ACTION_CHECK:
                    case HandAction.ACTION_CHECK_RAISE:
                        actionType = PlayerAction.CHECK;
                        break;
                    case HandAction.ACTION_CALL:
                        actionType = PlayerAction.CALL;
                        break;
                    case HandAction.ACTION_BET:
                        actionType = PlayerAction.BET;
                        break;
                    case HandAction.ACTION_RAISE:
                        actionType = PlayerAction.RAISE;
                        break;
                }

                if (actionType != HandAction.ACTION_NONE)
                {
                    playerActed(event.getPlayer(), actionType, action.getAmount());
                }

                break;

            case PokerTableEvent.TYPE_CARD_CHANGED:
                if (hasPocket) {
                    PokerPlayer player = event.getPlayer();

                    if (player != null)
                    {
                        dealtPockets();
                    }
                    else
                    {
                        HoldemHand hhand = event.getTable().getHoldemHand();

                        if (hhand != null)
                        {
                            Hand community = hhand.getCommunity();

                            if (community != null)
                            {
                                switch (community.size())
                                {
                                    case 5:
                                        dealtRiver();
                                        break;
                                    case 4:
                                        dealtTurn();
                                        break;
                                    case 3:
                                        dealtFlop();
                                        break;
                                }
                            }
                        }
                    }
                }
                break;
            case PokerTableEvent.TYPE_PLAYER_CHIPS_CHANGED:
                if (hasPocket) {

                    HoldemHand hhand = this.getPokerPlayer().getHoldemHand();
                    Hand community = hhand.getCommunity();

                    if (community == null)
                    {
                        dealtPockets();
                    }
                    else switch (community.size())
                    {
                        case 5:
                            dealtRiver();
                            break;
                        case 4:
                            dealtTurn();
                            break;
                        case 3:
                            dealtFlop();
                            break;
                    }
                }
                break;
        }
    }

    /**
     * May be overridden to return a Class object that DDPoker should instantiate for
     * configuring player types using this player implementation.  Returns null by
     * default, indicating that this class has no configurable options.
     */
    public Class getOptionsPanelClass()
    {
        return null;
    }

    /**
     * @param quick If true, decision-making speed is preferred over quality.
     * @return One of the following:
     *         <p/>
     *         <ul>
     *         <li>PlayerAction.fold()</li>
     *         <li>PlayerAction.check()</li>
     *         <li>PlayerAction.call(amount)</li>
     *         <li>PlayerAction.bet(amount)</li>
     *         <li>PlayerAction.raise(amount)</li>
     *         </ul>
     *         <p/>
     *         FOLD or CALL == CHECK, when applicable.
     *         <br>
     *         <br>
     *         If BET or RAISE is less than minimum, amount is adjusted upwards.
     *         <br>
     *         <br>
     *         If BET or RAISE exceeds player chips, amount is adjusted downwards.
     *         <br>
     *         <br>
     *         A bet or raise of zero == minimum bet/raise.
     *         <br>
     *         <br>
     *         With bets to call, BET of equal amount == CALL, greater amount == RAISE.
     */
    public PlayerAction getAction(boolean quick)
    {
        return PlayerAction.fold();
    }

    /**
     * Called when player has the option to rebuy.
     *
     * @return true if player wishes to rebuy.
     */
    public boolean wantsRebuy()
    {
        return false;
    }

    /**
     * Called when player has the option to add on.
     *
     * @return true if player wishes to add on.
     */
    public boolean wantsAddon()
    {
        return false;
    }

    /**
     * Is this player currently seated on the button?
     */
    public boolean isButton()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getSeat() == player.getTable().getButton();
    }

    /**
     * Is the player in a particular seat on the button?
     */
    public boolean isButton(int seat)
    {
        PokerTable table = getPokerPlayer().getTable();
        PokerPlayer player = table.getPlayer(seat);

        if (player == null)
        {
            return false;
        } else
        {
            return (seat == table.getButton());
        }
    }

    /**
     * Did this player post the small blind?
     */
    public boolean isSmallBlind()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getSeat() == player.getHoldemHand().getSmallBlindSeat();
    }

    /**
     * Did this player post the big blind?
     */
    public boolean isBigBlind()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getSeat() == player.getHoldemHand().getBigBlindSeat();
    }

    /**
     * Is this player in early position?
     */
    public boolean isEarlyPosition()
    {
        PokerPlayer player = getPokerPlayer();

        return player.isEarly();
    }

    /**
     * Is this player in middle position?
     */
    public boolean isMiddlePosition()
    {
        PokerPlayer player = getPokerPlayer();

        return player.isMiddle();
    }

    /**
     * Is this player in late position?
     */
    public boolean isLatePosition()
    {
        PokerPlayer player = getPokerPlayer();

        return player.isLate() && !isButton();
    }

    /**
     * Is the player in a particular seat in late position?
     */
    public boolean isLatePosition(int seat)
    {
        PokerTable table = getPokerPlayer().getTable();
        PokerPlayer player = table.getPlayer(seat);

        if (player == null)
        {
            return false;
        }
        else
        {
            return player.isLate() && (seat != table.getButton());
        }
    }

    /**
     * Has the pot been raised?
     */
    public boolean isPotRaised()
    {
        PokerPlayer player = getPokerPlayer();

        int potStatus = player.getHoldemHand().getPotStatus();

        return (potStatus == PokerConstants.RAISED_POT) || (potStatus == PokerConstants.RERAISED_POT);
    }

    /**
     * Has the pot been reraised?
     */
    public boolean isPotReraised()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getHoldemHand().getPotStatus() == PokerConstants.RERAISED_POT;
    }

    /**
     * Has the pot been called?  Pre-flop only.
     */
    public boolean isPotCalled()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getHoldemHand().getPotStatus() == PokerConstants.CALLED_POT;
    }

    /**
     * Has there been no pot action this round?
     */
    public boolean isNoPotAction()
    {
        PokerPlayer player = getPokerPlayer();

        return player.getHoldemHand().getPotStatus() == PokerConstants.NO_POT_ACTION;
    }

    protected void printMessage(String sMessage)
    {
        logger.debug(sMessage);
    }

    public int getBetAmount()
    {
        return 0;
    }

    public String getPlayerName()
    {
        return getPokerPlayer().getName();
    }

    public int getSeatNumber()
    {
        return getPokerPlayer().getSeat() + 1;
    }

    public void playerAdded(int seat)
    {
    }

    public void playerRemoved(int seat)
    {
    }

    public void newHand()
    {
    }

    public void dealtPockets()
    {
    }

    public void dealtFlop()
    {
    }

    public void dealtTurn()
    {
    }

    public void dealtRiver()
    {
    }

    public void blindsChanged()
    {
    }

    public void endHand()
    {
        PokerPlayer player = getPokerPlayer();

        HoldemHand hhand = player.getHoldemHand();

        if ((player.getHand() != null) && (hhand != null))
        {
            getOpponentModel().endHand(this, hhand, player);
        }
    }

    public void playerActed(PokerPlayer player, int action, int amount)
    {
    }

    /**
     * Which betting round is this currently.
     *
     * @return PRE_FLOP, FLOP, TURN, or RIVER.
     */
    public int getBettingRound()
    {
        return getPokerPlayer().getHoldemHand().getRound();
    }

    /**
     * Are we betting before flop?
     */
    public boolean isPreFlop()
    {
        return (getBettingRound() == PokerAI.PRE_FLOP);
    }

    /**
     * Are we betting after flop?
     */
    public boolean isFlop()
    {
        return (getBettingRound() == PokerAI.FLOP);
    }

    /**
     * Are we betting after turn?
     */
    public boolean isTurn()
    {
        return (getBettingRound() == PokerAI.TURN);
    }

    /**
     * Are we betting after river?
     */
    public boolean isRiver()
    {
        return (getBettingRound() == PokerAI.RIVER);
    }

    /**
     * Get this player's hole cards.
     */
    public Hand getPocket()
    {
        return getPokerPlayer().getHandSorted();
    }

    /**
     * Get the cards currently on the board.
     */
    public Hand getCommunity()
    {
        return getPokerPlayer().getHoldemHand().getCommunity();
    }

    public int getPotStatus()
    {
        return getPokerPlayer().getHoldemHand().getPotStatus();
    }

    /**
     * Returns the number of players who started the hand.
     */
    public int getNumPlayers()
    {
        return getPokerPlayer().getHoldemHand().getNumPlayers();
    }

    public int getRound()
    {
        HoldemHand hhand = getPokerPlayer().getHoldemHand();

        if (hhand == null)
        {
            return HoldemHand.ROUND_NONE;
        } else
        {
            return hhand.getRound();
        }
    }

    public int getAmountToCall()
    {
        return getPokerPlayer().getHoldemHand().getCall(getPokerPlayer());
    }

    /**
     * <code><pre>
     * Returns one of:
     *
     * POSITION_NONE
     * POSITION_EARLY
     * POSITION_MIDDLE
     * POSITION_LATE
     * POSITION_SMALL
     * POSITION_BIG
     *
     * 10 Seats Occupied:	E E E M M M L L S B
     *  9 Seats Occupied:	  E E M M M L L S B
     *  8 Seats Occupied:	    E M M M L L S B
     *  7 Seats Occupied:	      M M M L L S B
     *  6 Seats Occupied:	        M M L L S B
     *  5 Seats Occupied:	          M L L S B
     *  4 Seats Occupied:	            L L S B
     *  3 Seats Occupied:	              L S B
     *  2 Seats Occupied:	                B S
     * </pre></code>
     */
    public int getStartingPositionCategory()
    {
        return getStartingPositionCategory(getPokerPlayer());
    }

    /**
     * <code><pre>
     * Returns one of:
     *
     * POSITION_NONE
     * POSITION_EARLY
     * POSITION_MIDDLE
     * POSITION_LATE
     * POSITION_SMALL
     * POSITION_BIG
     *
     * 10 Seats Occupied:	E E E M M M L L S B
     *  9 Seats Occupied:	  E E M M M L L S B
     *  8 Seats Occupied:	    E M M M L L S B
     *  7 Seats Occupied:	      M M M L L S B
     *  6 Seats Occupied:	        M M L L S B
     *  5 Seats Occupied:	          M L L S B
     *  4 Seats Occupied:	            L L S B
     *  3 Seats Occupied:	              L S B
     *  2 Seats Occupied:	                B S
     * </pre></code>
     */
    public int getStartingPositionCategory(PokerPlayer player)
    {
        HoldemHand hhand = player.getHoldemHand();

        // no hand in play
        if ((hhand == null) || player.isFolded() || player.isAllIn()) return POSITION_NONE;

        int numPlayers = getNumPlayers();

        int nPosition;

        for (nPosition = 0; nPosition < numPlayers; ++nPosition)
        {
            if (hhand.getPlayerAt(nPosition) == player) break;
        }

        // not in hand
        if (nPosition == numPlayers) return POSITION_NONE;

        // special case for heads-up
        if (numPlayers == 2) return (nPosition == 0) ? POSITION_SMALL : POSITION_BIG;

        if (numPlayers - nPosition >= 8) return POSITION_EARLY;

        if (numPlayers - nPosition >= 5) return POSITION_MIDDLE;

        if (nPosition == (numPlayers - 2)) return POSITION_SMALL;

        if (nPosition == numPlayers - 1) return POSITION_BIG;

        if (numPlayers - nPosition >= 1) return POSITION_LATE;

        throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "Fall through in getStartingPositionCategory()", null);
    }

    /**
     * Returns the starting order of this player before the flop.
     * At a full table, under the gun returns 0, big blind 9.
     */
    public int getStartingOrder()
    {
        return getStartingOrder(getPokerPlayer());
    }

    /**
     * Returns the starting order of a player before the flop.
     * At a full table, under the gun returns 0, big blind 9.
     * @param player
     */ 
    public int getStartingOrder(PokerPlayer player)
    {
        HoldemHand hhand = player.getHoldemHand();

        // no hand in play
        if ((hhand == null) || player.isFolded() || player.isAllIn()) return POSITION_NONE;

        int numPlayers = getNumPlayers();

        int nPosition;

        for (nPosition = 0; nPosition < numPlayers; ++nPosition)
        {
            if (hhand.getPlayerAt(nPosition) == player) break;
        }

        return nPosition;
    }

    /**
     * <code><pre>
     * Returns one of:
     *
     * POSITION_EARLY
     * POSITION_MIDDLE
     * POSITION_LATE
     * POSITION_LAST
     *
     * 10 Players Left:	E E E M M M M L L T
     *  9 Players Left:	  E E E M M M L L T
     *  8 Players Left:	    E E M M M L L T
     *  7 Players Left:	      E E M M L L T
     *  6 Players Left:	        E E M M L T
     *  5 Players Left:	          E M M L T
     *  4 Players Left:	            M M L T
     *  3 Players Left:	              M L T
     *  2 Players Left:	                L T
     * </pre></code>
     */
    public int getPostFlopPositionCategory()
    {
        return getPostFlopPositionCategory(getPokerPlayer());
    }

    /**
     * <code><pre>
     * Returns one of:
     *
     * POSITION_EARLY
     * POSITION_MIDDLE
     * POSITION_LATE
     * POSITION_LAST
     *
     * 10 Players Left:	E E E M M M M L L T
     *  9 Players Left:	  E E E M M M L L T
     *  8 Players Left:	    E E M M M L L T
     *  7 Players Left:	      E E M M L L T
     *  6 Players Left:	        E E M M L T
     *  5 Players Left:	          E M M L T
     *  4 Players Left:	            M M L T
     *  3 Players Left:	              M L T
     *  2 Players Left:	                L T
     * </pre></code>
     */
    public int getPostFlopPositionCategory(PokerPlayer player)
    {
        HoldemHand hhand = player.getHoldemHand();

        // no hand in play, or out of hand
        if ((hhand == null) || player.isFolded() || player.isAllIn()) return POSITION_NONE;

        int numPlayers = getNumPlayers();

        int button = 0;

        // find the button
        for (int i = 0; i < numPlayers; ++i)
        {
            PokerPlayer p = hhand.getPlayerAt(i);

            if (p.getSeat() == hhand.getTable().getButton())
            {
                button = i;
                break;
            }
        }

        boolean found = false;

        int nPosition = 0;
        int numLeft = numPlayers;

        for (int i = 0; i < numPlayers; ++i)
        {
            PokerPlayer p = hhand.getPlayerAt((i + button + 1) % numPlayers);

            if (p == player) found = true;

            if (p.isFolded() || p.isAllIn()) --numLeft;

            else if (!found) ++nPosition;
        }

        // not in hand
        if (!found) return POSITION_NONE;

        switch (numLeft - nPosition)
        {
            case 1:
                return POSITION_LAST;
            case 2:
                return POSITION_LATE;
            case 3:
                return (numLeft > 6) ? POSITION_LATE : POSITION_MIDDLE;
            case 4:
                return POSITION_MIDDLE;
            case 5:
                return (numLeft > 6) ? POSITION_MIDDLE : POSITION_EARLY;
            case 6:
                return (numLeft > 7) ? POSITION_MIDDLE : POSITION_EARLY;
            case 7:
                return (numLeft > 9) ? POSITION_MIDDLE : POSITION_EARLY;
            case 8:
            case 9:
            case 10:
                return POSITION_EARLY;
        }

        throw new ApplicationError(ErrorCodes.ERROR_CODE_ERROR, "Fall through in getPostFlopPositionCategory()", null);
    }

    public String getStartingPositionDisplay()
    {
        switch (getStartingPositionCategory())
        {
            case POSITION_SMALL: return "Small Blind";
            case POSITION_BIG: return "Big Blind";
            case POSITION_EARLY: return "Early";
            case POSITION_MIDDLE: return "Middle";
            case POSITION_LATE: return isButton() ? "Button" : "Late";
        }

        return null;
    }

    public String getPostFlopPositionDisplay()
    {
        int nCat = getPostFlopPositionCategory();

        switch (nCat)
        {
            case POSITION_EARLY: return "Early";
            case POSITION_MIDDLE: return "Middle";
            case POSITION_LATE: return "Late";
            case POSITION_LAST: return "Last";
        }

        return null;
    }

    public int getChipCount()
    {
        return getPokerPlayer().getChipCount();
    }

    public int getBigBlindAmount()
    {
        return getPokerPlayer().getHoldemHand().getBigBlind();
    }

    public int getSmallBlindAmount()
    {
        return getPokerPlayer().getHoldemHand().getSmallBlind();
    }

    public int getTotalPotAmount()
    {
        return getPokerPlayer().getHoldemHand().getTotalPotChipCount();
    }

    public int getPlayersBefore()
    {
        return getPokerPlayer().getHoldemHand().getNumBefore(getPokerPlayer());
    }

    public int getPlayersLeftToAct()
    {
        return getPokerPlayer().getHoldemHand().getNumAfter(getPokerPlayer());
    }

    public int getBigBlindChipCount()
    {
        return getPokerPlayer().getHoldemHand().getBigBlindPlayer().getChipCount();
    }

    public Hand getHand()
    {
        return getPokerPlayer().getHand();
    }

    public int getNumAfter()
    {
        PokerPlayer player = getPokerPlayer();
        return player.getHoldemHand().getNumAfter(player);
    }

    public void demarshal(MsgState state, TokenizedList list)
    {
        init();

        super.demarshal(state, list);

        PokerSaveDetails pdetails = null;
        if (state != null && state instanceof GameState)
        {
            pdetails = (PokerSaveDetails) ((GameState)state).getSaveDetails().getCustomInfo();
        }

        String sPlayerType = list.removeStringToken();
        PlayerType playerType = PlayerType.getByUniqueKey(sPlayerType, pdetails);
        setPlayerType(playerType);

        if (list.peekToken() instanceof DMTypedHashMap)
        {
            map_ = (DMTypedHashMap)list.removeToken();
        }
        else
        {
            map_ = new DMTypedHashMap();
        }

        if (map_.containsKey("model.handsPlayed"))
        {
            //System.out.println("loading new style opponent model");
            getOpponentModel().loadFromMap(map_, "model.");
        }
        else
        {
            //System.out.println("loading old style opponent model");
            getOpponentModel().loadFromMap(map_, "om." + getPokerPlayer().getSeat() + ".");
        }
    }

    public void marshal(MsgState state, TokenizedList list)
    {
        super.marshal(state, list);

        getOpponentModel().saveToMap(map_, "model.");

        PlayerType playerType = getPlayerType();
        list.addToken(playerType == null ? (String) null : playerType.getUniqueKey());
        list.addToken(map_);
    }

    protected DMTypedHashMap getMap()
    {
        return map_;
    }

    public OpponentModel getOpponentModel()
    {
        return opponentModel_;
    }
}
