package cz.makub;

import com.clarkparsia.owlapi.explanation.DefaultExplanationGenerator;
import com.clarkparsia.owlapi.explanation.util.SilentExplanationProgressMonitor;
import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import com.clarkparsia.pellet.rules.model.Rule;
import com.google.common.collect.Multimap;

import cz.makub.Team;

import org.mindswap.pellet.KnowledgeBase;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrderer;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationOrdererImpl;
import uk.ac.manchester.cs.owl.explanation.ordering.ExplanationTree;
import uk.ac.manchester.cs.owl.explanation.ordering.Tree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * 
 * @author Calin Washington and Shubham Kahal
 */
public class Tutorial {

    static OWLOntologyManager manager;// =
                                      // OWLManager.createOWLOntologyManager();
    static File file;// = new File("nba_ontology.owl");
    static OWLOntology ontology;// =
                                // manager.loadOntologyFromOntologyDocument(file);
    static OWLObjectRenderer renderer;// = new DLSyntaxObjectRenderer();

    static OWLReasonerFactory reasonerFactory;// =
                                              // PelletReasonerFactory.getInstance();
    static OWLReasoner reasoner;// = reasonerFactory.createReasoner(ontology,
                                // new SimpleConfiguration());
    static OWLDataFactory factory;// = manager.getOWLDataFactory();
    static PrefixDocumentFormat pm;// =
                                   // manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();

    static void closeEm(Object... toClose) {
        for (Object obj : toClose) {
            if (obj != null) {
                try {
                    obj.getClass().getMethod("close").invoke(obj);
                } catch (Throwable t) {
                    System.out.println("Bad Log");
                }
            }
        }
    }

    public static void addTeams() {
        Connection cnc = null;

        try {
            cnc = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/PlayerData", "calin", "");

            Statement stm = null;
            ResultSet rst = null;
            ArrayList<Integer> list = new ArrayList<Integer>();

            try {
                stm = cnc.createStatement();
                rst = stm.executeQuery("select * from team where name like '%trail%'");
                Team cur;
                OWLIndividual team;
                OWLDataProperty name;
                OWLDataProperty abbrev;
                OWLDataProperty city;
                OWLDataProperty state;
                OWLDataPropertyAssertionAxiom nAxiom;
                OWLDataPropertyAssertionAxiom aAxiom;
                OWLDataPropertyAssertionAxiom cAxiom;
                OWLDataPropertyAssertionAxiom sAxiom;
                
                OWLClass teamClass = factory.getOWLClass("Team", pm);
                OWLAxiom assertClass;

                while (rst.next()) {
                    
                    cur = new Team(rst.getString(2), rst.getString(3),
                            rst.getString(4), rst.getString(5));
                    team = factory.getOWLNamedIndividual(cur.name.replaceAll(" ", "_"), pm);
                    
                    assertClass = factory.getOWLClassAssertionAxiom(teamClass, team);
                    manager.applyChange(new AddAxiom(ontology, assertClass));
                    
                    name = factory.getOWLDataProperty("Team_Name", pm);
                    abbrev = factory.getOWLDataProperty("Abbrev", pm);
                    city = factory.getOWLDataProperty("City", pm);
                    state = factory.getOWLDataProperty("State", pm);

                    nAxiom = factory.getOWLDataPropertyAssertionAxiom(name,
                            team, cur.name);
                    aAxiom = factory.getOWLDataPropertyAssertionAxiom(abbrev,
                            team, cur.abbrev);
                    cAxiom = factory.getOWLDataPropertyAssertionAxiom(city,
                            team, cur.city);
                    sAxiom = factory.getOWLDataPropertyAssertionAxiom(state,
                            team, cur.state);

                    AddAxiom addN = new AddAxiom(ontology, nAxiom);
                    AddAxiom addA = new AddAxiom(ontology, aAxiom);
                    AddAxiom addC = new AddAxiom(ontology, cAxiom);
                    AddAxiom addS = new AddAxiom(ontology, sAxiom);

                    manager.applyChange(addN);
                    manager.applyChange(addA);
                    manager.applyChange(addC);
                    manager.applyChange(addS);
                }

            } finally {
                closeEm(stm, rst);
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            closeEm(cnc);
        }
    }

    public static void addAwards() {
        Connection cnc = null;

        try {
            cnc = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/PlayerData", "calin", "");
            
            addAllNBA(cnc);
            addMVP(cnc);
            addSimpleAward(cnc, "allstar", "AllStar");
            addSimpleAward(cnc, "olympicmedals", "OlympicMedal");
            addSimpleAward(cnc, "playerofthemonth", "PlayerOfTheMonth");
            addSimpleAward(cnc, "playeroftheweek", "PlayerOfTheWeek");
            addROY(cnc);

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            closeEm(cnc);
        }
    }
    
    private static void addSimpleAward(Connection cnc, String tableName, String propertyName) throws SQLException {
        Statement stm = null;
        ResultSet rst = null;
        ArrayList<Integer> list = new ArrayList<Integer>();

        try {
            stm = cnc.createStatement();
            rst = stm.executeQuery("select * from " + tableName + " join player on pid = player_id");
            OWLIndividual player;
            OWLDataProperty data;
            OWLDataPropertyAssertionAxiom dataAxiom;
            int count;

            while (rst.next()) {
                count = rst.getInt("count");
                
                player = factory.getOWLNamedIndividual(
                        rst.getString("name"), pm);
                
                data = factory.getOWLDataProperty(propertyName, pm);
                
                dataAxiom = factory.getOWLDataPropertyAssertionAxiom(data,
                        player, count);
                
                manager.applyChange(new AddAxiom(ontology, dataAxiom));
            }

        } finally {
            closeEm(stm, rst);
        }
    }
    
    private static void addROY(Connection cnc) throws SQLException {
        Statement stm = null;
        ResultSet rst = null;
        ArrayList<Integer> list = new ArrayList<Integer>();

        try {
            stm = cnc.createStatement();
            rst = stm.executeQuery("select * from roy join player on pid = player_id");
            OWLIndividual player;
            OWLDataProperty data;
            OWLDataPropertyAssertionAxiom dataAxiom;

            while (rst.next()) {
                
                player = factory.getOWLNamedIndividual(
                        rst.getString("name"), pm);
                
                data = factory.getOWLDataProperty("ROY", pm);
                
                dataAxiom = factory.getOWLDataPropertyAssertionAxiom(data,
                        player, true);
                
                manager.applyChange(new AddAxiom(ontology, dataAxiom));
            }

        } finally {
            closeEm(stm, rst);
        }
    }

    private static void addMVP(Connection cnc) throws SQLException {
        Statement stm = null;
        ResultSet rst = null;
        ArrayList<Integer> list = new ArrayList<Integer>();

        try {
            stm = cnc.createStatement();
            rst = stm.executeQuery("select * from mvp join player on pid = player_id");
            OWLIndividual player;
            OWLDataProperty mvp;
            OWLDataPropertyAssertionAxiom mvpAxiom;
            String kind;
            int count;

            while (rst.next()) {
                kind = rst.getString("kind");
                count = rst.getInt("count");
                
                player = factory.getOWLNamedIndividual(
                        rst.getString("name"), pm);
                
                if (kind.equals("regular")) {
                    mvp = factory.getOWLDataProperty("MVP", pm);
                } else if (kind.equals("finals")) {
                    mvp = factory.getOWLDataProperty("MVP_Finals", pm);
                } else {
                    mvp = factory.getOWLDataProperty("MVP_AllStar", pm);
                }
                
                mvpAxiom = factory.getOWLDataPropertyAssertionAxiom(mvp,
                        player, count);
                
                manager.applyChange(new AddAxiom(ontology, mvpAxiom));
            }

        } finally {
            closeEm(stm, rst);
        }
    }

    private static void addAllNBA(Connection cnc) throws SQLException {
        Statement stm = null;
        ResultSet rst = null;
        ArrayList<Integer> list = new ArrayList<Integer>();

        try {
            stm = cnc.createStatement();
            rst = stm.executeQuery("select * from allnba join player on pid = player_id");
            OWLIndividual player;
            OWLDataProperty allnba;
            OWLDataPropertyAssertionAxiom allnbaAxiom;
            String kind;
            int count;

            while (rst.next()) {
                kind = rst.getString("kind");
                count = rst.getInt("count");
                
                player = factory.getOWLNamedIndividual(
                        rst.getString("name"), pm);
                
                if (kind.equals("first")) {
                    allnba = factory.getOWLDataProperty("AllNBA", pm);
                } else if (kind.equals("second")) {
                    allnba = factory.getOWLDataProperty("AllNBA_2nd", pm);
                } else if (kind.equals("third")) {
                    allnba = factory.getOWLDataProperty("AllNBA_3rd", pm);
                } else if (kind.equals("defense")) {
                    allnba = factory.getOWLDataProperty("AllDefense", pm);
                } else {
                    allnba = factory.getOWLDataProperty("AllRookie", pm);
                }
                
                allnbaAxiom = factory.getOWLDataPropertyAssertionAxiom(allnba,
                        player, count);
                
                manager.applyChange(new AddAxiom(ontology, allnbaAxiom));
            }

        } finally {
            closeEm(stm, rst);
        }
    }

    public static void addPlayers() {
        Connection cnc = null;

        try {
            cnc = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/PlayerData", "calin", "");

            Statement stm = null;
            ResultSet rst = null;
            ArrayList<Integer> list = new ArrayList<Integer>();

            try {
                stm = cnc.createStatement();
                rst = stm.executeQuery("select * from player");
                Player cur;

                OWLIndividual player;

                OWLDataProperty age;
                OWLDataProperty birthDate;
                OWLDataProperty experience;
                OWLDataProperty height;
                OWLDataProperty name;
                OWLDataProperty prior;
                OWLDataProperty weight;
                OWLDataProperty picUrl;
                OWLDataPropertyAssertionAxiom ageAxiom;
                OWLDataPropertyAssertionAxiom birthDateAxiom;
                OWLDataPropertyAssertionAxiom experienceAxiom;
                OWLDataPropertyAssertionAxiom heightAxiom;
                OWLDataPropertyAssertionAxiom nameAxiom;
                OWLDataPropertyAssertionAxiom priorAxiom;
                OWLDataPropertyAssertionAxiom weightAxiom;
                OWLDataPropertyAssertionAxiom picUrlAxiom;
                
                OWLClass playerClass = factory.getOWLClass("Player", pm);
                OWLAxiom assertClass;
                
                while (rst.next()) {

                    cur = new Player(rst.getString("age"),
                            rst.getString("birth_date"),
                            rst.getString("experience"),
                            rst.getString("height"), rst.getString("name"),
                            rst.getString("prior"), rst.getString("weight"),
                            rst.getString("pic_url"));

                    player = factory.getOWLNamedIndividual(cur.name.replaceAll(" ", "_"), pm);
                    
                    assertClass = factory.getOWLClassAssertionAxiom(playerClass, player);
                    manager.applyChange(new AddAxiom(ontology, assertClass));

                    age = factory.getOWLDataProperty("Age", pm);
                    birthDate = factory.getOWLDataProperty("Birthdate", pm);
                    experience = factory.getOWLDataProperty("YearsPlayed", pm);
                    height = factory.getOWLDataProperty("Height", pm);
                    name = factory.getOWLDataProperty("Player_Name", pm);
                    prior = factory.getOWLDataProperty("Prior", pm);
                    weight = factory.getOWLDataProperty("Weight", pm);
                    picUrl = factory.getOWLDataProperty("Pic_Url", pm);

                    ageAxiom = factory.getOWLDataPropertyAssertionAxiom(age,
                            player, new Integer(cur.age));
                    birthDateAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            birthDate, player, cur.birthDate);
                    experienceAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            experience, player, new Integer(cur.experience));
                    heightAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            height, player, cur.height);
                    nameAxiom = factory.getOWLDataPropertyAssertionAxiom(name,
                            player, cur.name);
                    priorAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            prior, player, cur.prior);
                    weightAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            weight, player, new Integer(cur.weight));
                    picUrlAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            picUrl, player, cur.picUrl);

                    AddAxiom addAge = new AddAxiom(ontology, ageAxiom);
                    AddAxiom addBirthDate = new AddAxiom(ontology,
                            birthDateAxiom);
                    AddAxiom addExperience = new AddAxiom(ontology,
                            experienceAxiom);
                    AddAxiom addHeight = new AddAxiom(ontology, heightAxiom);
                    AddAxiom addName = new AddAxiom(ontology, nameAxiom);
                    AddAxiom addPrior = new AddAxiom(ontology, priorAxiom);
                    AddAxiom addWeight = new AddAxiom(ontology, weightAxiom);
                    AddAxiom addPicUrl = new AddAxiom(ontology, picUrlAxiom);

                    manager.applyChange(addAge);
                    manager.applyChange(addBirthDate);
                    manager.applyChange(addExperience);
                    manager.applyChange(addHeight);
                    manager.applyChange(addName);
                    manager.applyChange(addPrior);
                    manager.applyChange(addWeight);
                    manager.applyChange(addPicUrl);
                }

            } finally {
                closeEm(stm, rst);
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            closeEm(cnc);
        }
    }
    

    private static void addChampionships() {
        Connection cnc = null;
        
        try {
            cnc = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/PlayerData", "calin", "");
        
            Statement stm = null;
            ResultSet rst = null;
    
            try {
                stm = cnc.createStatement();
                rst = stm.executeQuery("select * from championship join team on team_id = tid");
                OWLIndividual championship, team;
                OWLDataProperty year;
                OWLObjectProperty outcome;
                OWLDataPropertyAssertionAxiom yearAxiom, oldYearAxiom;
                OWLObjectPropertyAssertionAxiom outcomeAxiom;
    
                OWLClass championshipClass = factory.getOWLClass("Championship", pm);
                OWLAxiom assertClass;
                
                while (rst.next()) {
                    year = factory.getOWLDataProperty("Championship_Year", pm);
                    
                    if (rst.getString("kind").equals("Win")) {
                        outcome = factory.getOWLObjectProperty("Won", pm);
                    } else {
                        outcome = factory.getOWLObjectProperty("Lost", pm);
                    }
                    
                    team = factory.getOWLNamedIndividual(rst.getString("name").replaceAll(" ", "_"), pm);
                    championship = factory.getOWLNamedIndividual("Championship_" + rst.getString("year"), pm);
                    
                    assertClass = factory.getOWLClassAssertionAxiom(championshipClass, championship);
                    manager.applyChange(new AddAxiom(ontology, assertClass));
                    
                    // gives championship that year
                    yearAxiom = factory.getOWLDataPropertyAssertionAxiom(year,
                            championship, new Integer(rst.getString("year")));
                    
                    manager.applyChange(new AddAxiom(ontology, yearAxiom));
                    
                    outcomeAxiom = factory.getOWLObjectPropertyAssertionAxiom(outcome,
                            team, championship);
                    
                    manager.applyChange(new AddAxiom(ontology, outcomeAxiom));
                }
    
            } finally {
                closeEm(stm, rst);
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void addCareers() {
        Connection cnc = null;
        String individualName;

        try {
            cnc = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/PlayerData", "calin", "");

            Statement stm = null;
            ResultSet rst = null;

            try {
                stm = cnc.createStatement();
                rst = stm
                        .executeQuery("select * from careerstats join player on player_id = pid");
                Career cur;

                OWLObjectProperty playOverTheCourseOfTheir;
                OWLObjectPropertyAssertionAxiom playOverTheCourseOfTheirAxiom;

                OWLIndividual career, player;
                OWLDataProperty ast;
                OWLDataProperty blk;
                OWLDataProperty dReb;
                OWLDataProperty fgPercentage;
                OWLDataProperty fga;
                OWLDataProperty fgm;
                OWLDataProperty ftPercentage;
                OWLDataProperty fta;
                OWLDataProperty ftm;
                OWLDataProperty gp;
                OWLDataProperty gs;
                OWLDataProperty minutes;
                OWLDataProperty oReb;
                OWLDataProperty pf;
                OWLDataProperty pts;
                OWLDataProperty reb;
                OWLDataProperty stl;
                OWLDataProperty threePa;
                OWLDataProperty threePm;
                OWLDataProperty threePointPercentage;
                OWLDataProperty tov;
                OWLDataPropertyAssertionAxiom astAxiom;
                OWLDataPropertyAssertionAxiom blkAxiom;
                OWLDataPropertyAssertionAxiom dRebAxiom;
                OWLDataPropertyAssertionAxiom fgPercentageAxiom;
                OWLDataPropertyAssertionAxiom fgaAxiom;
                OWLDataPropertyAssertionAxiom fgmAxiom;
                OWLDataPropertyAssertionAxiom ftPercentageAxiom;
                OWLDataPropertyAssertionAxiom ftaAxiom;
                OWLDataPropertyAssertionAxiom ftmAxiom;
                OWLDataPropertyAssertionAxiom gpAxiom;
                OWLDataPropertyAssertionAxiom gsAxiom;
                OWLDataPropertyAssertionAxiom minutesAxiom;
                OWLDataPropertyAssertionAxiom oRebAxiom;
                OWLDataPropertyAssertionAxiom pfAxiom;
                OWLDataPropertyAssertionAxiom ptsAxiom;
                OWLDataPropertyAssertionAxiom rebAxiom;
                OWLDataPropertyAssertionAxiom stlAxiom;
                OWLDataPropertyAssertionAxiom threePaAxiom;
                OWLDataPropertyAssertionAxiom threePmAxiom;
                OWLDataPropertyAssertionAxiom threePointPercentageAxiom;
                OWLDataPropertyAssertionAxiom tovAxiom;
                
                OWLClass careerClass = factory.getOWLClass("Career", pm);
                OWLAxiom assertClass;

                while (rst.next()) {
                    String playerName = rst.getString("player.name");

                    cur = new Career(rst.getString("ast"),
                            rst.getString("blk"), rst.getString("d_reb"),
                            rst.getString("fg_percentage"),
                            rst.getString("fga"), rst.getString("fgm"),
                            rst.getString("ft_percentage"),
                            rst.getString("fta"), rst.getString("ftm"),
                            rst.getString("gp"), rst.getString("gs"),
                            rst.getString("minutes"), rst.getString("o_reb"),
                            rst.getString("pf"), rst.getString("pts"),
                            rst.getString("reb"), rst.getString("stl"),
                            rst.getString("three_pa"),
                            rst.getString("three_pm"),
                            rst.getString("three_point_percentage"),
                            rst.getString("tov"));

                    individualName = rst.getString("name").replaceAll(" ", "_") + "_" + "career";
                    career = factory.getOWLNamedIndividual(individualName, pm);
                    
                    assertClass = factory.getOWLClassAssertionAxiom(careerClass, career);
                    manager.applyChange(new AddAxiom(ontology, assertClass));

                    // OBJECT PROPERTIES
                    playOverTheCourseOfTheir = factory.getOWLObjectProperty(
                            "PlayOverTheCourseOfTheir", pm);
                    player = factory.getOWLNamedIndividual(playerName, pm);
                    playOverTheCourseOfTheirAxiom = factory
                            .getOWLObjectPropertyAssertionAxiom(
                                    playOverTheCourseOfTheir, player, career);
                    manager.applyChange(new AddAxiom(ontology,
                            playOverTheCourseOfTheirAxiom));

                    // DATA PROPERTIES
                    ast = factory.getOWLDataProperty("Assists", pm);
                    blk = factory.getOWLDataProperty("Blocks", pm);
                    dReb = factory.getOWLDataProperty("D_Reb", pm);
                    fgPercentage = factory.getOWLDataProperty("FG_Percent", pm);

                    ast = factory.getOWLDataProperty("Assists", pm);
                    blk = factory.getOWLDataProperty("Blocks", pm);
                    dReb = factory.getOWLDataProperty("D_Rebs", pm);
                    fgPercentage = factory.getOWLDataProperty("FG_Percent", pm);
                    fga = factory.getOWLDataProperty("FGA", pm);
                    fgm = factory.getOWLDataProperty("FGM", pm);
                    ftPercentage = factory.getOWLDataProperty("FT_Percent", pm);
                    fta = factory.getOWLDataProperty("FTA", pm);
                    ftm = factory.getOWLDataProperty("FTM", pm);
                    gp = factory.getOWLDataProperty("GP", pm);
                    gs = factory.getOWLDataProperty("GS", pm);
                    minutes = factory.getOWLDataProperty("Minutes", pm);
                    oReb = factory.getOWLDataProperty("O_Rebs", pm);
                    pf = factory.getOWLDataProperty("PersonalFouls", pm);
                    pts = factory.getOWLDataProperty("Points", pm);
                    reb = factory.getOWLDataProperty("Rebounds", pm);
                    stl = factory.getOWLDataProperty("Steals", pm);
                    threePa = factory.getOWLDataProperty("3PA", pm);
                    threePm = factory.getOWLDataProperty("3PM", pm);
                    threePointPercentage = factory.getOWLDataProperty(
                            "3P_Percent", pm);
                    tov = factory.getOWLDataProperty("Turnovers", pm);

                    astAxiom = factory.getOWLDataPropertyAssertionAxiom(ast,
                            career, new Double(cur.ast));
                    blkAxiom = factory.getOWLDataPropertyAssertionAxiom(blk,
                            career, new Double(cur.blk));
                    dRebAxiom = factory.getOWLDataPropertyAssertionAxiom(dReb,
                            career, new Double(cur.dReb));
                    fgPercentageAxiom = factory
                            .getOWLDataPropertyAssertionAxiom(fgPercentage,
                                    career, new Double(cur.fgPercentage));
                    fgaAxiom = factory.getOWLDataPropertyAssertionAxiom(fga,
                            career, new Double(cur.fga));
                    fgmAxiom = factory.getOWLDataPropertyAssertionAxiom(fgm,
                            career, new Double(cur.fgm));
                    ftPercentageAxiom = factory
                            .getOWLDataPropertyAssertionAxiom(ftPercentage,
                                    career, new Double(cur.ftPercentage));
                    ftaAxiom = factory.getOWLDataPropertyAssertionAxiom(fta,
                            career, new Double(cur.fta));
                    ftmAxiom = factory.getOWLDataPropertyAssertionAxiom(ftm,
                            career, new Double(cur.ftm));
                    gpAxiom = factory.getOWLDataPropertyAssertionAxiom(gp,
                            career, new Integer(cur.gp));
                    gsAxiom = factory.getOWLDataPropertyAssertionAxiom(gs,
                            career, new Integer(cur.gs));
                    minutesAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            minutes, career, new Double(cur.minutes));
                    oRebAxiom = factory.getOWLDataPropertyAssertionAxiom(oReb,
                            career, new Double(cur.oReb));
                    pfAxiom = factory.getOWLDataPropertyAssertionAxiom(pf,
                            career, new Double(cur.pf));
                    ptsAxiom = factory.getOWLDataPropertyAssertionAxiom(pts,
                            career, new Double(cur.pts));
                    rebAxiom = factory.getOWLDataPropertyAssertionAxiom(reb,
                            career, new Double(cur.reb));
                    stlAxiom = factory.getOWLDataPropertyAssertionAxiom(stl,
                            career, new Double(cur.stl));
                    threePaAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            threePa, career, new Double(cur.threePa));
                    threePmAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            threePm, career, new Double(cur.threePm));
                    threePointPercentageAxiom = factory
                            .getOWLDataPropertyAssertionAxiom(
                                    threePointPercentage, career,
                                    new Double(cur.threePointPercentage));
                    tovAxiom = factory.getOWLDataPropertyAssertionAxiom(tov,
                            career, new Double(cur.tov));

                    AddAxiom addAst = new AddAxiom(ontology, astAxiom);
                    AddAxiom addBlk = new AddAxiom(ontology, blkAxiom);
                    AddAxiom addDReb = new AddAxiom(ontology, dRebAxiom);
                    AddAxiom addFGPercent = new AddAxiom(ontology,
                            fgPercentageAxiom);
                    AddAxiom addFGA = new AddAxiom(ontology, fgaAxiom);
                    AddAxiom addFGM = new AddAxiom(ontology, fgmAxiom);
                    AddAxiom addFTPercent = new AddAxiom(ontology,
                            ftPercentageAxiom);
                    AddAxiom addFTA = new AddAxiom(ontology, ftaAxiom);
                    AddAxiom addFTM = new AddAxiom(ontology, ftmAxiom);
                    AddAxiom addGP = new AddAxiom(ontology, gpAxiom);
                    AddAxiom addGS = new AddAxiom(ontology, gsAxiom);
                    AddAxiom addMinutes = new AddAxiom(ontology, minutesAxiom);
                    AddAxiom addOReb = new AddAxiom(ontology, oRebAxiom);
                    AddAxiom addPF = new AddAxiom(ontology, pfAxiom);
                    AddAxiom addPts = new AddAxiom(ontology, ptsAxiom);
                    AddAxiom addReb = new AddAxiom(ontology, rebAxiom);
                    AddAxiom addStl = new AddAxiom(ontology, stlAxiom);
                    AddAxiom addThreePA = new AddAxiom(ontology, threePaAxiom);
                    AddAxiom addThreePM = new AddAxiom(ontology, threePmAxiom);
                    AddAxiom addThreePercent = new AddAxiom(ontology,
                            threePointPercentageAxiom);
                    AddAxiom addTOV = new AddAxiom(ontology, tovAxiom);

                    manager.applyChange(addAst);
                    manager.applyChange(addBlk);
                    manager.applyChange(addDReb);
                    manager.applyChange(addFGPercent);
                    manager.applyChange(addFGA);
                    manager.applyChange(addFGM);
                    manager.applyChange(addFTPercent);
                    manager.applyChange(addFTA);
                    manager.applyChange(addFTM);
                    manager.applyChange(addGP);
                    manager.applyChange(addGS);
                    manager.applyChange(addMinutes);
                    manager.applyChange(addOReb);
                    manager.applyChange(addPF);
                    manager.applyChange(addPts);
                    manager.applyChange(addReb);
                    manager.applyChange(addStl);
                    manager.applyChange(addThreePA);
                    manager.applyChange(addThreePM);
                    manager.applyChange(addThreePercent);
                    manager.applyChange(addTOV);
                }

            } finally {
                closeEm(stm, rst);
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            closeEm(cnc);
        }
    }

    public static void addSeasons() {
        Connection cnc = null;
        String individualName;

        try {
            cnc = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/PlayerData", "calin", "");

            Statement stm = null;
            ResultSet rst = null;
            ArrayList<Integer> list = new ArrayList<Integer>();

            try {
                stm = cnc.createStatement();
                rst = stm.executeQuery("select * from seasonstats join player on player_id = pid join team on tid = team_id");
                Season cur;
                OWLIndividual season, team, player;

                OWLObjectProperty playedFor;
                OWLObjectProperty playsFor;
                OWLObjectProperty playDuringIndividual;
                OWLObjectPropertyAssertionAxiom playedForAxiom;
                OWLObjectPropertyAssertionAxiom playsForAxiom;
                OWLObjectPropertyAssertionAxiom playDuringIndividualAxiom;

                OWLDataProperty age;
                OWLDataProperty year;
                OWLDataProperty ast;
                OWLDataProperty blk;
                OWLDataProperty dReb;
                OWLDataProperty fgPercentage;
                OWLDataProperty fga;
                OWLDataProperty fgm;
                OWLDataProperty ftPercentage;
                OWLDataProperty fta;
                OWLDataProperty ftm;
                OWLDataProperty gp;
                OWLDataProperty gs;
                OWLDataProperty minutes;
                OWLDataProperty oReb;
                OWLDataProperty pf;
                OWLDataProperty pts;
                OWLDataProperty reb;
                OWLDataProperty stl;
                OWLDataProperty threePa;
                OWLDataProperty threePm;
                OWLDataProperty threePointPercentage;
                OWLDataProperty tov;
                OWLDataPropertyAssertionAxiom ageAxiom;
                OWLDataPropertyAssertionAxiom yearAxiom;
                OWLDataPropertyAssertionAxiom astAxiom;
                OWLDataPropertyAssertionAxiom blkAxiom;
                OWLDataPropertyAssertionAxiom dRebAxiom;
                OWLDataPropertyAssertionAxiom fgPercentageAxiom;
                OWLDataPropertyAssertionAxiom fgaAxiom;
                OWLDataPropertyAssertionAxiom fgmAxiom;
                OWLDataPropertyAssertionAxiom ftPercentageAxiom;
                OWLDataPropertyAssertionAxiom ftaAxiom;
                OWLDataPropertyAssertionAxiom ftmAxiom;
                OWLDataPropertyAssertionAxiom gpAxiom;
                OWLDataPropertyAssertionAxiom gsAxiom;
                OWLDataPropertyAssertionAxiom minutesAxiom;
                OWLDataPropertyAssertionAxiom oRebAxiom;
                OWLDataPropertyAssertionAxiom pfAxiom;
                OWLDataPropertyAssertionAxiom ptsAxiom;
                OWLDataPropertyAssertionAxiom rebAxiom;
                OWLDataPropertyAssertionAxiom stlAxiom;
                OWLDataPropertyAssertionAxiom threePaAxiom;
                OWLDataPropertyAssertionAxiom threePmAxiom;
                OWLDataPropertyAssertionAxiom threePointPercentageAxiom;
                OWLDataPropertyAssertionAxiom tovAxiom;
                
                OWLClass seasonClass = factory.getOWLClass("Season", pm);
                OWLAxiom assertClass;

                while (rst.next()) {
                    
                    String teamName = rst.getString("team.name").replaceAll(" ", "_");
                    String playerName = rst.getString("player.name").replaceAll(" ", "_");

                    cur = new Season(rst.getString("age"),
                            rst.getString("season"), rst.getString("ast"),
                            rst.getString("blk"), rst.getString("d_reb"),
                            rst.getString("fg_percentage"),
                            rst.getString("fga"), rst.getString("fgm"),
                            rst.getString("ft_percentage"),
                            rst.getString("fta"), rst.getString("ftm"),
                            rst.getString("gp"), rst.getString("gs"),
                            rst.getString("minutes"), rst.getString("o_reb"),
                            rst.getString("pf"), rst.getString("pts"),
                            rst.getString("reb"), rst.getString("stl"),
                            rst.getString("three_pa"),
                            rst.getString("three_pm"),
                            rst.getString("three_point_percentage"),
                            rst.getString("tov"));

                    individualName = rst.getString("name").replaceAll(" ", "_") + "_" + "season"
                            + "_" + cur.year;
                    season = factory.getOWLNamedIndividual(individualName, pm);
                    
                    assertClass = factory.getOWLClassAssertionAxiom(seasonClass, season);
                    manager.applyChange(new AddAxiom(ontology, assertClass));

                    // OBJECT PROPERTIES
                    playedFor = factory.getOWLObjectProperty("PlayedFor", pm);
                    team = factory.getOWLNamedIndividual(teamName, pm);
                    playedForAxiom = factory
                            .getOWLObjectPropertyAssertionAxiom(playedFor,
                                    season, team);
                    manager.applyChange(new AddAxiom(ontology, playedForAxiom));

                    playDuringIndividual = factory.getOWLObjectProperty(
                            "PlayDuringIndividual", pm);
                    player = factory.getOWLNamedIndividual(playerName, pm);
                    playDuringIndividualAxiom = factory
                            .getOWLObjectPropertyAssertionAxiom(
                                    playDuringIndividual, player, season);
                    manager.applyChange(new AddAxiom(ontology,
                            playDuringIndividualAxiom));

                    if (cur.year.equals("2016")) {
                        playsFor = factory.getOWLObjectProperty("PlaysFor", pm);
                        playsForAxiom = factory
                                .getOWLObjectPropertyAssertionAxiom(playsFor,
                                        player, team);
                        manager.applyChange(new AddAxiom(ontology,
                                playsForAxiom));
                    }

                    // DATA PROPERTIES
                    age = factory.getOWLDataProperty("Age", pm);
                    year = factory.getOWLDataProperty("Season_Year", pm);
                    ast = factory.getOWLDataProperty("Assists", pm);
                    blk = factory.getOWLDataProperty("Blocks", pm);
                    dReb = factory.getOWLDataProperty("D_Rebs", pm);
                    fgPercentage = factory.getOWLDataProperty("FG_Percent", pm);
                    fga = factory.getOWLDataProperty("FGA", pm);
                    fgm = factory.getOWLDataProperty("FGM", pm);
                    ftPercentage = factory.getOWLDataProperty("FT_Percent", pm);
                    fta = factory.getOWLDataProperty("FTA", pm);
                    ftm = factory.getOWLDataProperty("FTM", pm);
                    gp = factory.getOWLDataProperty("GP", pm);
                    gs = factory.getOWLDataProperty("GS", pm);
                    minutes = factory.getOWLDataProperty("Minutes", pm);
                    oReb = factory.getOWLDataProperty("O_Rebs", pm);
                    pf = factory.getOWLDataProperty("PersonalFouls", pm);
                    pts = factory.getOWLDataProperty("Points", pm);
                    reb = factory.getOWLDataProperty("Rebounds", pm);
                    stl = factory.getOWLDataProperty("Steals", pm);
                    threePa = factory.getOWLDataProperty("3PA", pm);
                    threePm = factory.getOWLDataProperty("3PM", pm);
                    threePointPercentage = factory.getOWLDataProperty(
                            "3P_Percent", pm);
                    tov = factory.getOWLDataProperty("Turnovers", pm);

                    ageAxiom = factory.getOWLDataPropertyAssertionAxiom(age,
                            season, new Integer(cur.age));
                    yearAxiom = factory.getOWLDataPropertyAssertionAxiom(year,
                            season, new Integer(cur.year));
                    astAxiom = factory.getOWLDataPropertyAssertionAxiom(ast,
                            season, new Double(cur.ast));
                    blkAxiom = factory.getOWLDataPropertyAssertionAxiom(blk,
                            season, new Double(cur.blk));
                    dRebAxiom = factory.getOWLDataPropertyAssertionAxiom(dReb,
                            season, new Double(cur.dReb));
                    fgPercentageAxiom = factory
                            .getOWLDataPropertyAssertionAxiom(fgPercentage,
                                    season, new Double(cur.fgPercentage));
                    fgaAxiom = factory.getOWLDataPropertyAssertionAxiom(fga,
                            season, new Double(cur.fga));
                    fgmAxiom = factory.getOWLDataPropertyAssertionAxiom(fgm,
                            season, new Double(cur.fgm));
                    ftPercentageAxiom = factory
                            .getOWLDataPropertyAssertionAxiom(ftPercentage,
                                    season, new Double(cur.ftPercentage));
                    ftaAxiom = factory.getOWLDataPropertyAssertionAxiom(fta,
                            season, new Double(cur.fta));
                    ftmAxiom = factory.getOWLDataPropertyAssertionAxiom(ftm,
                            season, new Double(cur.ftm));
                    gpAxiom = factory.getOWLDataPropertyAssertionAxiom(gp,
                            season, new Integer(cur.gp));
                    gsAxiom = factory.getOWLDataPropertyAssertionAxiom(gs,
                            season, new Integer(cur.gs));
                    minutesAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            minutes, season, new Double(cur.minutes));
                    oRebAxiom = factory.getOWLDataPropertyAssertionAxiom(oReb,
                            season, new Double(cur.oReb));
                    pfAxiom = factory.getOWLDataPropertyAssertionAxiom(pf,
                            season, new Double(cur.pf));
                    ptsAxiom = factory.getOWLDataPropertyAssertionAxiom(pts,
                            season, new Double(cur.pts));
                    rebAxiom = factory.getOWLDataPropertyAssertionAxiom(reb,
                            season, new Double(cur.reb));
                    stlAxiom = factory.getOWLDataPropertyAssertionAxiom(stl,
                            season, new Double(cur.stl));
                    threePaAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            threePa, season, new Double(cur.threePa));
                    threePmAxiom = factory.getOWLDataPropertyAssertionAxiom(
                            threePm, season, new Double(cur.threePm));
                    threePointPercentageAxiom = factory
                            .getOWLDataPropertyAssertionAxiom(
                                    threePointPercentage, season,
                                    new Double(cur.threePointPercentage));
                    tovAxiom = factory.getOWLDataPropertyAssertionAxiom(tov,
                            season, new Double(cur.tov));

                    AddAxiom addAge = new AddAxiom(ontology, ageAxiom);
                    AddAxiom addYear = new AddAxiom(ontology, yearAxiom);
                    AddAxiom addAst = new AddAxiom(ontology, astAxiom);
                    AddAxiom addBlk = new AddAxiom(ontology, blkAxiom);
                    AddAxiom addDReb = new AddAxiom(ontology, dRebAxiom);
                    AddAxiom addFGPercent = new AddAxiom(ontology,
                            fgPercentageAxiom);
                    AddAxiom addFGA = new AddAxiom(ontology, fgaAxiom);
                    AddAxiom addFGM = new AddAxiom(ontology, fgmAxiom);
                    AddAxiom addFTPercent = new AddAxiom(ontology,
                            ftPercentageAxiom);
                    AddAxiom addFTA = new AddAxiom(ontology, ftaAxiom);
                    AddAxiom addFTM = new AddAxiom(ontology, ftmAxiom);
                    AddAxiom addGP = new AddAxiom(ontology, gpAxiom);
                    AddAxiom addGS = new AddAxiom(ontology, gsAxiom);
                    AddAxiom addMinutes = new AddAxiom(ontology, minutesAxiom);
                    AddAxiom addOReb = new AddAxiom(ontology, oRebAxiom);
                    AddAxiom addPF = new AddAxiom(ontology, pfAxiom);
                    AddAxiom addPts = new AddAxiom(ontology, ptsAxiom);
                    AddAxiom addReb = new AddAxiom(ontology, rebAxiom);
                    AddAxiom addStl = new AddAxiom(ontology, stlAxiom);
                    AddAxiom addThreePA = new AddAxiom(ontology, threePaAxiom);
                    AddAxiom addThreePM = new AddAxiom(ontology, threePmAxiom);
                    AddAxiom addThreePercent = new AddAxiom(ontology,
                            threePointPercentageAxiom);
                    AddAxiom addTOV = new AddAxiom(ontology, tovAxiom);

                    manager.applyChange(addAge);
                    manager.applyChange(addYear);
                    manager.applyChange(addAst);
                    manager.applyChange(addBlk);
                    manager.applyChange(addDReb);
                    manager.applyChange(addFGPercent);
                    manager.applyChange(addFGA);
                    manager.applyChange(addFGM);
                    manager.applyChange(addFTPercent);
                    manager.applyChange(addFTA);
                    manager.applyChange(addFTM);
                    manager.applyChange(addGP);
                    manager.applyChange(addGS);
                    manager.applyChange(addMinutes);
                    manager.applyChange(addOReb);
                    manager.applyChange(addPF);
                    manager.applyChange(addPts);
                    manager.applyChange(addReb);
                    manager.applyChange(addStl);
                    manager.applyChange(addThreePA);
                    manager.applyChange(addThreePM);
                    manager.applyChange(addThreePercent);
                    manager.applyChange(addTOV);
                }

            } finally {
                closeEm(stm, rst);
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            closeEm(cnc);
        }
    }

    private static final String BASE_URL = "http://www.semanticweb.org/calin/ontologies/2016/3/";

    public static void main(String[] args) throws OWLOntologyCreationException {

        // prepare ontology and reasoner
        manager = OWLManager.createOWLOntologyManager();
        file = new File("nba_ontology.owl");
        ontology = manager.loadOntologyFromOntologyDocument(file);
        renderer = new DLSyntaxObjectRenderer();

        reasonerFactory = PelletReasonerFactory.getInstance();
        //reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        factory = manager.getOWLDataFactory();
        pm = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
        pm.setDefaultPrefix(BASE_URL + "#");
    
        addTeams();
        addPlayers();
        addSeasons();
        addCareers();
        addAwards();
        addChampionships();
        
        file = new File("nba_data.owl");
        IRI documentIRI2 = IRI.create(file);
        
        //new Interface(ontology, pm).run(); //why do i have to do this first????
        
        try {
            manager.saveOntology(ontology, new OWLXMLDocumentFormat(), documentIRI2);
        } catch (OWLOntologyStorageException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        //new Interface().run();
        
        System.exit(0); //running out of memory here.. way around?
        
        reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        
        ((PelletReasoner)reasoner).getKB().realize();
        
        InferredOntologyGenerator generator = new InferredOntologyGenerator(reasoner);
        generator.fillOntology(factory, ontology);
        
        try {
            manager.saveOntology(ontology, new OWLXMLDocumentFormat(), documentIRI2);
        } catch (OWLOntologyStorageException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // save in OWL/XML format
        /*
        try {
            manager.saveOntology(ontology, new StreamDocumentTarget(System.out));
        //change this to IRI.create(new File("nba_ontology.owl").toURI())
        } catch (OWLOntologyStorageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

        // get class and its individuals
        /*OWLClass personClass = factory.getOWLClass(":Player", pm);
        
        System.out.println(personClass);
        System.out.println(reasoner.getInstances(personClass,
                false).getFlattened());

        for (OWLNamedIndividual player : reasoner.getInstances(personClass,
                false).getFlattened()) {
            System.out.println("Player : " + renderer.render(player));
        }*/

        // get a given individual
        OWLNamedIndividual martin = factory.getOWLNamedIndividual(":Kobe", pm);
        // get values of selected properties on the individual
        OWLDataProperty hasEmailProperty = factory.getOWLDataProperty(
                ":hasEmail", pm);

        OWLObjectProperty isEmployedAtProperty = factory.getOWLObjectProperty(
                ":isEmployedAt", pm);

        for (OWLLiteral email : reasoner.getDataPropertyValues(martin,
                hasEmailProperty)) {
            System.out.println("Martin has email: " + email.getLiteral());
        }

        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(martin,
                isEmployedAtProperty).getFlattened()) {
            System.out
                    .println("Martin is employed at: " + renderer.render(ind));
        }

        // get labels
        LocalizedAnnotationSelector as = new LocalizedAnnotationSelector(
                ontology, factory, "en", "cs");
        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(martin,
                isEmployedAtProperty).getFlattened()) {
            System.out.println("Martin is employed at: '" + as.getLabel(ind)
                    + "'");
        }

        // get inverse of a property, i.e. which individuals are in relation
        // with a given individual
        OWLNamedIndividual university = factory
                .getOWLNamedIndividual(":MU", pm);
        OWLObjectPropertyExpression inverse = factory
                .getOWLObjectInverseOf(isEmployedAtProperty);
        for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(
                university, inverse).getFlattened()) {
            System.out.println("MU inverseOf(isEmployedAt) -> "
                    + renderer.render(ind));
        }

        // find to which classes the individual belongs
        Collection<OWLClassExpression> assertedClasses = EntitySearcher
                .getTypes(martin, ontology);
        for (OWLClass c : reasoner.getTypes(martin, false).getFlattened()) {
            boolean asserted = assertedClasses.contains(c);
            System.out.println((asserted ? "asserted" : "inferred")
                    + " class for Martin: " + renderer.render(c));
        }

        // list all object property values for the individual
        Multimap<OWLObjectPropertyExpression, OWLIndividual> assertedValues = EntitySearcher
                .getObjectPropertyValues(martin, ontology);
        for (OWLObjectProperty objProp : ontology
                .getObjectPropertiesInSignature(Imports.INCLUDED)) {
            for (OWLNamedIndividual ind : reasoner.getObjectPropertyValues(
                    martin, objProp).getFlattened()) {
                boolean asserted = assertedValues.get(objProp).contains(ind);
                System.out.println((asserted ? "asserted" : "inferred")
                        + " object property for Martin: "
                        + renderer.render(objProp) + " -> "
                        + renderer.render(ind));
            }
        }

        // list all same individuals
        for (OWLNamedIndividual ind : reasoner.getSameIndividuals(martin)) {
            System.out.println("same as Martin: " + renderer.render(ind));
        }

        // ask reasoner whether Martin is employed at MU
        boolean result = reasoner.isEntailed(factory
                .getOWLObjectPropertyAssertionAxiom(isEmployedAtProperty,
                        martin, university));
        System.out.println("Is Martin employed at MU ? : " + result);

        // check whether the SWRL rule is used
        OWLNamedIndividual ivan = factory.getOWLNamedIndividual(":Ivan", pm);
        OWLClass chOMPClass = factory.getOWLClass(":ChildOfMarriedParents", pm);
        OWLClassAssertionAxiom axiomToExplain = factory
                .getOWLClassAssertionAxiom(chOMPClass, ivan);
        System.out.println("Is Ivan child of married parents ? : "
                + reasoner.isEntailed(axiomToExplain));

        // explain why Ivan is child of married parents
        DefaultExplanationGenerator explanationGenerator = new DefaultExplanationGenerator(
                manager, reasonerFactory, ontology, reasoner,
                new SilentExplanationProgressMonitor());
        Set<OWLAxiom> explanation = explanationGenerator
                .getExplanation(axiomToExplain);
        ExplanationOrderer deo = new ExplanationOrdererImpl(manager);
        ExplanationTree explanationTree = deo.getOrderedExplanation(
                axiomToExplain, explanation);
        System.out.println();
        System.out
                .println("-- explanation why Ivan is in class ChildOfMarriedParents --");
        printIndented(explanationTree, "");
    }

    private static void printIndented(Tree<OWLAxiom> node, String indent) {
        OWLAxiom axiom = node.getUserObject();
        System.out.println(indent + renderer.render(axiom));
        if (!node.isLeaf()) {
            for (Tree<OWLAxiom> child : node.getChildren()) {
                printIndented(child, indent + "    ");
            }
        }
    }

    /**
     * Helper class for extracting labels, comments and other anotations in
     * preffered languages. Selects the first literal annotation matching the
     * given languages in the given order.
     */
    @SuppressWarnings("WeakerAccess")
    public static class LocalizedAnnotationSelector {
        private final List<String> langs;
        private final OWLOntology ontology;
        private final OWLDataFactory factory;

        /**
         * Constructor.
         * 
         * @param ontology
         *            ontology
         * @param factory
         *            data factory
         * @param langs
         *            list of prefered languages; if none is provided the
         *            Locale.getDefault() is used
         */
        public LocalizedAnnotationSelector(OWLOntology ontology,
                OWLDataFactory factory, String... langs) {
            this.langs = (langs == null) ? Collections.singletonList(Locale
                    .getDefault().toString()) : Arrays.asList(langs);
            this.ontology = ontology;
            this.factory = factory;
        }

        /**
         * Provides the first label in the first matching language.
         * 
         * @param ind
         *            individual
         * @return label in one of preferred languages or null if not available
         */
        public String getLabel(OWLNamedIndividual ind) {
            return getAnnotationString(ind,
                    OWLRDFVocabulary.RDFS_LABEL.getIRI());
        }

        @SuppressWarnings("UnusedDeclaration")
        public String getComment(OWLNamedIndividual ind) {
            return getAnnotationString(ind,
                    OWLRDFVocabulary.RDFS_COMMENT.getIRI());
        }

        public String getAnnotationString(OWLNamedIndividual ind,
                IRI annotationIRI) {
            return getLocalizedString(EntitySearcher.getAnnotations(ind,
                    ontology, factory.getOWLAnnotationProperty(annotationIRI)));
        }

        private String getLocalizedString(Collection<OWLAnnotation> annotations) {
            List<OWLLiteral> literalLabels = new ArrayList<>(annotations.size());
            for (OWLAnnotation label : annotations) {
                if (label.getValue() instanceof OWLLiteral) {
                    literalLabels.add((OWLLiteral) label.getValue());
                }
            }
            for (String lang : langs) {
                for (OWLLiteral literal : literalLabels) {
                    if (literal.hasLang(lang))
                        return literal.getLiteral();
                }
            }
            for (OWLLiteral literal : literalLabels) {
                if (!literal.hasLang())
                    return literal.getLiteral();
            }
            return null;
        }
    }
}