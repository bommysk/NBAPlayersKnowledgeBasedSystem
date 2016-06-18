package cz.makub;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.clarkparsia.pellet.rules.model.Rule;

public class Interface {
    private OWLOntologyManager manager;
    private OWLOntology ontology;
    private OWLObjectRenderer renderer;
    private OWLReasonerFactory reasonerFactory;
    private OWLReasoner reasoner;
    private OWLDataFactory factory;
    private PrefixDocumentFormat pm;
    private HashMap<String, OWLDataProperty> questionBooleanMap = new HashMap<String, OWLDataProperty>();
    private HashMap<String, OWLDataProperty> questionCountMap = new HashMap<String, OWLDataProperty>();
    private HashMap<String, OWLDataProperty> questionValueMap = new HashMap<String, OWLDataProperty>();
    private HashMap<String, Integer> questionTargetMap = new HashMap<String, Integer>();
    
    /**
     * question -> boolean data property
     * question -> count data property
     * question -> value data property map
     *      question -> target value
     * unique methods for object properties
     */
    
    //QUESTIONS
    public String scorer = "Have they consistently been a great scorer in the NBA?";
    public String pickpocket = "Are they known as a pickpocket?";
    public String shotblocker = "Are they known as a shot blocker?";
    public String defender = "Are they known as a defender?";
    public String playMaker = "Are they known as a great passer?";
    public String shooter = "Are they known as a shooter?";
    public String foulDrawer = "Does this player draw a lot of fouls?";
    public String goodFreeThrow = "Are they a good free throw shooter?";
    
    public String champion = "Have they won a championship?";
    public String finalsLoser = "Have they lost a championship?";
    public String multiFinalsAppearances = "Have they appeared in multiple championship series?";
    public String olympicMedal = "Have they won an Olympic medal?";
    
    public String veteran = "Are they a veteran of the NBA?";
    public String rookie = "Are they currently a rookie?";
    public String over30 = "Are they over the age of 30?";
    public String under20 = "Are they under the age of 20?";
    
    public String scrub = "Are they scrub?";
    public String rolePlayer = "Are they a role player?";
    public String importantPlayer = "Are they an important player to their team?";
    
    public String allnba = "Have they ever been in the All NBA?";
    public String alldefense = "Have they ever been in the NBA All Defensive Team?";
    public String allnba2nd = "Have they ever been in the All NBA 2nd Team?";
    public String allnba3rd = "Have they ever been in the All NBA 3rd Team?";
    public String allrookie = "Have they ever been in the All Rookie Team?";
    public String allstar = "Have they ever been an Allstar?";
    
    public String western = "Are they playing in the Western Conference?";
    
    public String forward = "Are they a forward?";
    public String guard = "Are they a guard?";
    public String center = "Are they a center?";
    
    public String allstarMVP = "Have they won All Star MVP?";
    public String regularMVP = "Have they won regular season MVP?";
    public String finalsMVP = "Have they won Finals MVP?";
    
    public String outOfPrime = "Are they out of their prime?";
    public String playerOfWeek10 = "Have they won more than 10 Player of the Week awards?";
    public String playerOfTheMonth = "Have they won more than 10 Player of the Month awards?";
    
    public String persistentOnOneTeam = "Have they been persistently on one team?";
    
    public String teammates = "Have they ever been teammates with "; //add player name
    public String whatTeam = "Do they play for the "; //add team name
    
    private ArrayList<OWLNamedIndividual> players, remaining; 
    private ArrayList<String> questionList = new ArrayList<String>();
    
    public Interface(OWLOntology ontology, PrefixDocumentFormat pm) throws OWLOntologyCreationException {
        manager = OWLManager.createOWLOntologyManager();
        this.ontology = ontology;//manager.loadOntologyFromOntologyDocument(file);
        renderer = new DLSyntaxObjectRenderer();
        reasonerFactory = PelletReasonerFactory.getInstance();
        factory = manager.getOWLDataFactory();
        this.pm = pm;//manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
        reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        players = new ArrayList<OWLNamedIndividual>();
        
        fillQuestionList();
    }
    
    private static final String BASE_URL = "http://www.semanticweb.org/calin/ontologies/2016/3/";
    
    public Interface() throws OWLOntologyCreationException {
        manager = OWLManager.createOWLOntologyManager();
        File file = new File("AllNBAData.owl");
        this.ontology = manager.loadOntologyFromOntologyDocument(file);
        renderer = new DLSyntaxObjectRenderer();
        reasonerFactory = PelletReasonerFactory.getInstance();
        factory = manager.getOWLDataFactory();
        this.pm = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
        pm.setDefaultPrefix(BASE_URL + "#");
        reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
    }
    
    public void fillQuestionList() {
        /// BOOLEANS
        questionBooleanMap.put(scorer, factory.getOWLDataProperty("Scorer", pm));
        questionList.add(scorer);
        
        //questionBooleanMap.put(pickpocket, factory.getOWLDataProperty("PickPocket", pm));  
        //questionList.add(pickpocket);
        
        questionBooleanMap.put(shotblocker, factory.getOWLDataProperty("Shotblocker", pm));
        questionList.add(scorer);
        
        questionBooleanMap.put(defender, factory.getOWLDataProperty("Defender", pm));
        questionList.add(defender);
        
        questionBooleanMap.put(playMaker, factory.getOWLDataProperty("Playmaker", pm));
        questionList.add(playMaker);
        
        questionBooleanMap.put(shooter, factory.getOWLDataProperty("Shooter", pm));     
        questionList.add(shooter);
        
        questionBooleanMap.put(foulDrawer, factory.getOWLDataProperty("FoulDrawer", pm));      
        questionList.add(foulDrawer);
        
        questionBooleanMap.put(veteran, factory.getOWLDataProperty("Veteran", pm)); 
        questionList.add(veteran);
        
        questionBooleanMap.put(olympicMedal, factory.getOWLDataProperty("OlympicMedal", pm));
        questionList.add(olympicMedal);
        
        questionBooleanMap.put(scrub, factory.getOWLDataProperty("Scrub", pm));
        questionList.add(scrub);
        
        questionBooleanMap.put(rolePlayer, factory.getOWLDataProperty("Role_Player", pm));
        questionList.add(rolePlayer);
        
        questionBooleanMap.put(importantPlayer, factory.getOWLDataProperty("Important_Player", pm));
        questionList.add(importantPlayer);
        
        questionBooleanMap.put(allnba, factory.getOWLDataProperty("AllNBA", pm));
        questionList.add(allnba);
        
        questionBooleanMap.put(alldefense, factory.getOWLDataProperty("AllDefense", pm));
        questionList.add(alldefense);
        
        questionBooleanMap.put(allnba2nd, factory.getOWLDataProperty("AllNBA_2nd", pm));
        questionList.add(allnba2nd);
        
        questionBooleanMap.put(allnba3rd, factory.getOWLDataProperty("AllNBA_3rd", pm));
        questionList.add(allnba3rd);
        
        questionBooleanMap.put(allrookie, factory.getOWLDataProperty("AllRookie", pm));
        questionList.add(allrookie);
        
        questionBooleanMap.put(allstar, factory.getOWLDataProperty("AllStar", pm));
        questionList.add(allstar);
        
        questionBooleanMap.put(allstarMVP, factory.getOWLDataProperty("MVP_AllStar", pm));
        questionList.add(allstarMVP);
        
        questionBooleanMap.put(regularMVP, factory.getOWLDataProperty("MVP", pm));
        questionList.add(regularMVP);
        
        questionBooleanMap.put(finalsMVP, factory.getOWLDataProperty("MVP_Finals", pm));
        questionList.add(finalsMVP);

        questionBooleanMap.put(playerOfWeek10, factory.getOWLDataProperty("PlayerOfTheWeek", pm));
        questionList.add(playerOfWeek10);
        
        questionBooleanMap.put(playerOfTheMonth, factory.getOWLDataProperty("PlayerOfTheMonth", pm));
        questionList.add(playerOfTheMonth);
        
        /// OBJECT
        
        
        /*questionPropertyMapObj.put(champion, factory.getOWLObjectProperty("Won", pm));
        questionList.add(champion);
        
        questionPropertyMapObj.put(finalsLoser, factory.getOWLObjectProperty("Lost", pm));
        questionList.add(finalsLoser);
        
        questionPropertyMapObj.put(multiFinalsAppearances, factory.getOWLObjectProperty("PlayedIn", pm));
        questionList.add(multiFinalsAppearances);/*
        
        
        // VALUE + TARGET
        /*questionList.add(goodFreeThrow); // we don't have property for this so we need to store it
        
        questionPropertyMap.put(rookie, factory.getOWLDataProperty("YearsPlayed", pm)); 
        questionList.add(rookie);
        
        questionPropertyMap.put(over30, factory.getOWLDataProperty("Age", pm)); // how to check this get age first and filter 
        questionList.add(over30);
        
        questionPropertyMap.put(under20, factory.getOWLDataProperty("Age", pm)); // how to check this get age first and filter
        questionList.add(under20);*/
        
        // questionPropertyMap.put(persistentOnOneTeam, factory.getOWLDataProperty("PersistentOnOneTeam", pm));
        // questionList.add(persistentOnOneTeam);
        
        // questionPropertyMap.put(teammates, factory.getOWLDataProperty("Center", pm)); teammates
        // questionPropertyMap.put(whatTeam, factory.getOWLDataProperty("Center", pm)); team
    }
    
    // returns result set size if answer to question is yes
    public int getResultSetSize(ArrayList<OWLNamedIndividual> remaining, OWLDataProperty property) {
        int sizeCounter = 0;
        
        for (OWLNamedIndividual player : remaining) {
            Set<OWLLiteral> res = reasoner.getDataPropertyValues(player, property);
            sizeCounter += res.size(); //will be 1 if present at all and 0 otherwise
        }
      
        return sizeCounter;
    }
    
    public String determineQuestion(ArrayList<String> questionsAsked) {
        String questionToAsk = "";
        int remainingSize = remaining.size();
        int resSize, finalSize = remainingSize;
        int curCounter = remainingSize;
        int target;
        
        for (String question : questionList) {
            if (!questionsAsked.contains(question)) {
                resSize = getResultSetSize(remaining, questionBooleanMap.get(question));
                target = Math.abs(resSize - (remainingSize / 2));
                
                if (target < curCounter) {
                    curCounter = target; 
                    questionToAsk = question;
                    finalSize = resSize;
                }
            }
        }
        
        //everything getting removed so try data based questions
        if (finalSize == remainingSize || finalSize == 0) {
            /*if (persistentOnOneTeam()) { //return true if players aren't equally persistent
                return persistentOnOneTeam;
            } else*/ 
            if ((questionToAsk = currentTeam()) != null) { //null if no valid question, else question about specific team to ask about
                return currentTeam();
            } else {
                return null;
            }
        }
        
        return questionToAsk;
    }
    
    /*
    public boolean persistentOnOneTeam() {
        int total = -1;
        
        for (OWLNamedIndividual player : remaining) {
            Set<OWLLiteral> res = reasoner.getDataPropertyValues(player, factory.getOWLDataProperty("PlayedFor", pm));
            if (total == -1) {
                total = res.size();
            } else if (res.size() != total) {
                return true;
            }
        }
        
        return false;
    }
    */
    
    public String currentTeam() {
        HashMap<String, Integer> teamCount = new HashMap<String, Integer>();
        OWLClass teamClass = factory.getOWLClass("Team", pm);
        String cur, bestTeam = "";
        int count, best, target;
        
        /*
        for(OWLNamedIndividual team : reasoner.getInstances(teamClass, false).getFlattened()) {
            teamCount.put(renderer.render(team).trim(), 0);
        }
        */
        
        for (OWLNamedIndividual player : remaining) {
            NodeSet<OWLNamedIndividual> res = reasoner.getObjectPropertyValues(player, factory.getOWLObjectProperty("PlaysFor", pm));
            for (Node<OWLNamedIndividual> teamNode : res.getNodes()) {
                cur = teamNode.getEntities().toArray()[0].toString();
                cur = cur.substring(53, cur.length() - 1);
                count = teamCount.get(cur.trim()) == null ? 0 : teamCount.get(cur.trim());
                teamCount.put(cur.trim(), count + 1);
            }
        }
        
        best = remaining.size();
        
        for (String team : teamCount.keySet()) {
            count = teamCount.get(team);
            target = Math.abs(count - (best / 2));
            
            if (target < best) {
                best = target; 
                bestTeam = team;
            }
        }
        
        return "Have they played for the " + bestTeam + "?";
    }
    
    public void run() {
        
        players = new ArrayList<OWLNamedIndividual>();
        
        OWLClass playerClass = factory.getOWLClass("Player", pm);
        
        for(OWLNamedIndividual player : reasoner.getInstances(playerClass, false).getFlattened()) {
            /*Set<OWLLiteral> res = reasoner.getDataPropertyValues(player, factory.getOWLDataProperty("PickPocket", pm));
            
            if (res.size() > 0) {
                System.out.println(renderer.render(player));
            }*/
            
            players.add(player);
        }
        
        remaining = new ArrayList<OWLNamedIndividual>(players);
        
        int questionCount = 0;
        ArrayList<String> questionsAsked = new ArrayList<String>();
        boolean curAnswer;
        String questionToAsk;
        ArrayList<String> names;
        
        fillQuestionList();
        System.out.println("Think of a player then answer questions by entering y or n:");
        
        while (remaining.size() > 1 && questionsAsked.size() < questionList.size()) {
            
            System.out.println("_________________________");
            names = new ArrayList<String>();
            for(OWLNamedIndividual player : remaining) {
                names.add(formatName(renderer.render(player)));
            }
            
            Collections.sort(names);
            
            for (String name : names) {
                System.out.println(name);
            }
            
            
            System.out.println("_________________________");
            System.out.println("left: " + remaining.size());
            
            questionToAsk = determineQuestion(questionsAsked); // determine question to ask
            
            if (questionToAsk == null) {
                break;
            }
            
            questionsAsked.add(questionToAsk);
            curAnswer = getResponse(questionToAsk);
            if (questionToAsk.contains("played for")) {
                filterPlayersByTeam(questionToAsk, curAnswer);
            }
            else {
                filterPlayers(questionBooleanMap.get(questionToAsk), curAnswer);
            }
            
            questionCount++;
        }
        System.out.println("left: " + remaining.size());
        
        if (remaining.size() == 1) {
            System.out.println("Your player must be " + formatName(renderer.render(remaining.remove(0))));
            System.out.println("Player found in " + questionCount + " questions");
        } else if (remaining.size() > 1) {
            System.out.println("Your player must be one of the following:");
            
            for(OWLNamedIndividual player : remaining) {
                System.out.println(formatName(renderer.render(player)));
            }
            
            System.out.println("Players found in " + questionCount + " questions");
        } else {            
            System.out.println("No matching player found :(");
        }
    }
    
    public void filterPlayers(OWLDataProperty property, boolean answer) {
        ArrayList<OWLNamedIndividual> toRemove = new ArrayList<OWLNamedIndividual>();
        
        for (OWLNamedIndividual player : remaining) {
            Set<OWLLiteral> res = reasoner.getDataPropertyValues(player, property);
            
            if (answer && res.size() == 0) { //remove where property is false //empty if not set for player so remove
                toRemove.add(player);
            } else if (!answer && res.size() > 0){ //remove where property is true
                toRemove.add(player);
            }
        }
        
        remaining.removeAll(toRemove);
        System.out.println("removed: " + toRemove.size());
    }
    
    public void filterPlayersByTeam(String question, boolean answer) {
        ArrayList<OWLNamedIndividual> toRemove = new ArrayList<OWLNamedIndividual>();
        String cur;
        String team = question.substring(24, question.length() - 1);
        
        
        for (OWLNamedIndividual player : remaining) {
            NodeSet<OWLNamedIndividual> res = reasoner.getObjectPropertyValues(player, factory.getOWLObjectProperty("PlaysFor", pm));
            for (Node<OWLNamedIndividual> teamNode : res.getNodes()) {
                cur = teamNode.getEntities().toArray()[0].toString();
                cur = cur.substring(53, cur.length() - 1);
                
                if (answer && !cur.trim().equals(team.trim())) {
                    toRemove.add(player);
                }
                else if (!answer && cur.trim().equals(team.trim())) {
                    toRemove.add(player);
                }
            }
        }
        
        remaining.removeAll(toRemove);
        System.out.println("removed: " + toRemove.size());
    }
    
    public boolean getResponse(String question) {
        System.out.println(question);
        Scanner scan = new Scanner(System.in);
        String s = scan.nextLine();
        
        if (s.equals("y") || s.equals("Y")) {
            return true;
        } else if (s.equals("n") || s.equals("N")) {
            return false;
        } else {
            System.out.println("format response correctly...");
            System.out.println("_________________");
            return getResponse(question);
        }
    }
    
    public String formatName(String name) {
        String names[] = name.split("_");
        String ret = "";
        names[0] = Character.toUpperCase(names[0].charAt(0)) + names[0].substring(1);
        
        for(int i=0; i<names.length; i++) {
            ret += Character.toUpperCase(names[i].charAt(0)) + names[i].substring(1) + " ";
        }
        
        return ret.trim();
    }
    
    public static void main(String args[]) {
        try {
            new Interface().run();
        } catch (OWLOntologyCreationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
