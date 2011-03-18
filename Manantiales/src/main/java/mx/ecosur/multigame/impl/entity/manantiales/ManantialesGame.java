/*
 * Copyright (C) 2010 ECOSUR, Andrew Waterman
 *
 * Licensed under the Academic Free License v. 3.2.
 * http://www.opensource.org/licenses/afl-3.0.php
 */

package mx.ecosur.multigame.impl.entity.manantiales;

import mx.ecosur.multigame.enums.GameState;

import mx.ecosur.multigame.exception.InvalidMoveException;
import mx.ecosur.multigame.exception.InvalidRegistrationException;

import mx.ecosur.multigame.grid.Color;
import mx.ecosur.multigame.grid.MoveComparator;
import mx.ecosur.multigame.grid.model.*;

import mx.ecosur.multigame.impl.enums.manantiales.ConditionType;
import mx.ecosur.multigame.impl.enums.manantiales.Mode;

import mx.ecosur.multigame.impl.util.manantiales.ManantialesMessageSender;
import mx.ecosur.multigame.model.interfaces.*;
import mx.ecosur.multigame.MessageSender;

import javax.persistence.*;

import org.drools.io.Resource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.io.ResourceFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.KnowledgeBaseFactory;
import org.drools.KnowledgeBase;

import java.util.*;
import java.net.MalformedURLException;

@Entity
public class ManantialesGame extends GridGame {
        
    private static final long serialVersionUID = -8395074059039838349L;

    private static String FNAME = "ManantialesKBase.dat";

    private static KnowledgeBase kbase;
        
    private Mode mode;
        
    private Set<CheckCondition> checkConditions;

    private transient ManantialesMessageSender messageSender;

    private Set<PuzzleSuggestion> suggestions;

    private Map<Mode,MoveHistory> moveHistory;

    private Color[] colors = { Color.YELLOW, Color.RED, Color.BLACK, Color.PURPLE };

    public ManantialesGame () {
        super();
        mode = Mode.CLASSIC;
        moveHistory = new HashMap<Mode,MoveHistory>();
    }

    public ManantialesGame (KnowledgeBase kbase) {
        super();
        this.kbase = kbase;
    }

    public ManantialesGame (Mode mode) {
        super();
        this.mode = mode;
    }

    public void initialize() throws MalformedURLException {
        this.setGrid(new GameGrid());
        this.setState(GameState.BEGIN);
        this.setCreated(new Date());
        this.setColumns(9);
        this.setRows(9);

        if (kbase == null)
            kbase = findKBase();

        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession();
        session.setGlobal("messageSender", getMessageSender());
        session.insert(this);
        for (Object fact : getFacts()) {
            session.insert(fact);
        }

        session.getAgenda().getAgendaGroup("initialize").setFocus();
        session.fireAllRules();
        session.dispose();
    }

    @Override
    @Transient
    public Set getFacts() {
        Set facts = super.getFacts();
        if (checkConditions != null)
            facts.addAll(checkConditions);
        return facts;
    }

    @Override
    @Transient
    public List<Color> getColors() {
        List<Color> ret = new ArrayList<Color>();
        for (Color color : colors) {
            ret.add(color);
        }
        return ret;
    }

    @Transient
    public String getChangeSet() {
        return "/mx/ecosur/multigame/impl/manantiales.xml";
    }

    @Enumerated (EnumType.STRING)
    public Mode getMode() {
        return mode;
    }

    public void setMode (Mode mode) {
        this.checkConditions = null;
        if (getMoves() != null && mode != null) {
            MoveHistory history = new MoveHistory ();
            history.setMode(mode);
            history.setMoves(getMoves());
            getMoveHistory().put(mode, history);
        }
        this.mode = mode;
    }

    //@OneToMany (cascade={CascadeType.ALL}, fetch=FetchType.EAGER)
    //@MapKey (name="mode")
    @Transient
    public Map<Mode, MoveHistory> getMoveHistory() {
        if (moveHistory == null) {
            moveHistory = new HashMap<Mode,MoveHistory>();
        }
        return moveHistory;
    }

    public void setMoveHistory(Map<Mode, MoveHistory> moveHistory) {
        this.moveHistory = moveHistory;
    }

    //@OneToMany (cascade={CascadeType.ALL}, fetch=FetchType.EAGER)
    @Transient
    public Set<CheckCondition> getCheckConditions () {
        if (checkConditions == null)
                checkConditions = new LinkedHashSet<CheckCondition>();
        return checkConditions;
    }
    
    public void setCheckConditions (Set<CheckCondition> checkConstraints) {
        this.checkConditions = checkConstraints;
    }

    @Transient
    public boolean hasCondition (ConditionType type) {
        boolean ret = false;
        if (checkConditions != null) {
            for (CheckCondition condition : checkConditions) {
                if (condition.getType().equals(type)) {
                    ret = true;}}}
        return ret;
    }

    @Transient
    public MessageSender getMessageSender() {
        if (messageSender == null) {
            messageSender = new ManantialesMessageSender ();
            messageSender.initialize();
        }
        return messageSender;
    }

    public void setMessageSender(MessageSender messageSender) {
        if (messageSender instanceof ManantialesMessageSender)
            this.messageSender = (ManantialesMessageSender) messageSender;
    }

    //@OneToMany (cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @Transient
    public Set<PuzzleSuggestion> getSuggestions () {
        if (suggestions == null)
            suggestions = new LinkedHashSet<PuzzleSuggestion>();
        return suggestions;

    }

    public void setSuggestions (Set<PuzzleSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    public void updateSuggestion (PuzzleSuggestion suggestion) {
        if (suggestions != null) {
            for (PuzzleSuggestion possible : suggestions) {
                if (possible.equals(suggestion)) {
                    possible.setStatus(suggestion.getStatus());
                }
            }
        }
    }

    @Transient
    public Resource getResource() {
        return ResourceFactory.newInputStreamResource(getClass().getResourceAsStream (
            getChangeSet()));
    }

    @Transient
    public String getGameType() {
        return "Manantiales";
    }

    @Deprecated
    protected KnowledgeBase findKBase () {
        KnowledgeBase ret = KnowledgeBaseFactory.newKnowledgeBase();
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newInputStreamResource(getClass().getResourceAsStream (
            "/mx/ecosur/multigame/impl/manantiales.xml")), ResourceType.CHANGE_SET);
        ret.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return ret;
    }

    @Transient
    public int getMaxPlayers() {
        return 4;
    }

    /* (non-Javadoc)
      * @see GridGame#move(mx.ecosur.multigame.model.interfaces.Move)
      */
    @Override
    public Move move(Move impl) throws InvalidMoveException {
        ManantialesMove move = (ManantialesMove) impl;

        if (kbase == null)
            kbase = findKBase();
        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession();
        session.setGlobal("messageSender", getMessageSender()); 
        session.insert(this);
        session.insert(move);
        for (Object fact : getFacts()) {
            session.insert(fact);
        }

        session.getAgenda().getAgendaGroup("verify").setFocus();
        session.fireAllRules();
        session.getAgenda().getAgendaGroup("move").setFocus();
        session.fireAllRules();
        session.getAgenda().getAgendaGroup("evaluate").setFocus();
        session.fireAllRules();
        session.dispose();

        getMoves().add(move);

        return move;
    }

    @Override
    public Suggestion suggest (Suggestion suggestion) {
        if (kbase == null)
            kbase = findKBase();        
        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession();
        session.setGlobal("messageSender", getMessageSender());
        session.insert(this);
        session.insert(suggestion);
        for (Object fact : getFacts())
            session.insert(fact);
        getSuggestions().add((PuzzleSuggestion) suggestion);
        session.getAgenda().getAgendaGroup("verify").setFocus();
        session.fireAllRules();
        session.getAgenda().getAgendaGroup("move").setFocus();
        session.fireAllRules();
        session.getAgenda().getAgendaGroup("evaluate").setFocus();
        session.fireAllRules();
        session.dispose();
        return suggestion;
    }

    public GamePlayer registerPlayer(Registrant registrant) throws InvalidRegistrationException  {
        ManantialesPlayer player = new ManantialesPlayer ();
        player.setRegistrant((GridRegistrant) registrant);

        for (GridPlayer p : this.getPlayers()) {
            if (p.equals (player))
                throw new InvalidRegistrationException ("Duplicate Registraton!");
        }

        int max = getMaxPlayers();
        if (players.size() == max)
            throw new RuntimeException ("Maximum Players reached!");

        List<Color> colors = getAvailableColors();
        player.setColor(colors.get(0));
        players.add(player);

        try {
            if (players.size() == getMaxPlayers())
                initialize();
        } catch (MalformedURLException e) {
                throw new InvalidRegistrationException (e);
        }

        if (this.created == null)
            this.setCreated(new Date());
        if (this.state == null)
                this.state = GameState.WAITING;

        return player;
    }
        
    public Agent registerAgent (Agent agent) throws InvalidRegistrationException {
        SimpleAgent player = (SimpleAgent) agent;

        for (GridPlayer p : this.getPlayers()) {
            if (p.equals (player))
                throw new InvalidRegistrationException ("Duplicate Registraton!");
        }

        int max = getMaxPlayers();
        if (players.size() == max)
            throw new RuntimeException ("Maximum Players reached!");

        List<Color> colors = getAvailableColors();
        player.setColor(colors.get(0));
        players.add(player);

        if (players.size() == getMaxPlayers())
        try {
            initialize();
        } catch (MalformedURLException e) {
            throw new InvalidRegistrationException (e);
        }

        if (this.created == null)
            this.setCreated(new Date());
        if (this.state == null)
            this.state = GameState.WAITING;

        return player;        
    }

    @Override
    public String toString() {
        return getGrid().toString();
    }

    /* (non-Javadoc)
     * @see GridGame#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        ManantialesGame ret = new ManantialesGame();
        ret.grid = new GameGrid();
        for (GridCell cell : getGrid().getCells()) {
            ManantialesFicha ficha = (ManantialesFicha) cell;
            ret.grid.updateCell((GridCell) ficha.clone());
        }

        ret.setColumns (this.getColumns());
        ret.setRows (this.getRows());
        ret.created = new Date(System.currentTimeMillis());
        ret.id = this.getId();
        ret.moves = new TreeSet<GridMove>(new MoveComparator());
        ret.state = this.state;
        ret.setMode(this.mode);
        ret.version = this.version;
        ret.kbase = findKBase();
        return ret;
    }
}
