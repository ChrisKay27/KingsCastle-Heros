package com.kingscastle.level;

import android.graphics.RectF;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.Log;

import com.kingscastle.effects.animations.DecoAnimation;
import com.kingscastle.framework.GameTime;
import com.kingscastle.framework.Rpg;
import com.kingscastle.gameElements.GameElement;
import com.kingscastle.gameElements.GenericGameElement;
import com.kingscastle.gameElements.Tree;
import com.kingscastle.gameElements.livingThings.LivingThing;
import com.kingscastle.gameElements.livingThings.LivingThingListenerAdapter;
import com.kingscastle.gameElements.livingThings.SoldierTypes.Humanoid;
import com.kingscastle.gameElements.livingThings.abilities.DamageBuff;
import com.kingscastle.gameElements.livingThings.abilities.Haste;
import com.kingscastle.gameElements.livingThings.army.HumanSoldier;
import com.kingscastle.gameElements.livingThings.army.HumanWizard;
import com.kingscastle.gameUtils.Difficulty;
import com.kingscastle.gameUtils.vector;
import com.kingscastle.heroes.PlayerAchievements;
import com.kingscastle.level.Heroes.BuffPickup;
import com.kingscastle.level.Heroes.ForestSpawns;
import com.kingscastle.level.Heroes.Pickup;
import com.kingscastle.level.Heroes.TripleAttackPickup;
import com.kingscastle.teams.HumanPlayer;
import com.kingscastle.teams.Team;
import com.kingscastle.teams.Teams;
import com.kingscastle.teams.races.HumanRace;
import com.kingscastle.teams.races.UndeadRace;
import com.kingscastle.util.ManagerListener;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Chris on 7/12/2015 for TowerDefence
 */
public abstract class HeroesLevel extends Level{
    private static final String TAG = HeroesLevel.class.getSimpleName();
    public static final String DONT_ADD_SCORE = "DontAddScore";

    protected HumanWizard hero;

    private int startOnRound = 1;
    private long checkMonstersArePathingAt;
    private Difficulty difficulty;
    protected final List<RectF> noBuildZones = new ArrayList<>();
    private boolean roundActive = true;

    private long playersScore;
    private final PlayerAchievements playerAchievements = new PlayerAchievements();

    private List<Pickup> pickups = new LinkedList<>();

    @Override
    protected void subOnCreate() {
        createBorder();
        hPlayer = new HumanPlayer();
        mm.add(Team.getNewHumanInstance(hPlayer, new HumanRace(), mm.getGridUtil()));

        hPlayer.setLives((int) (hPlayer.getLives() / difficulty.multiplier));

        mm.add(Team.getNewInstance(Teams.RED, new UndeadRace(), mm.getGridUtil()));

        hPlayer.addLVCL(new HumanPlayer.onLivesValueChangedListener() {
            @Override
            public void onLivesValueChanged(int newLivesValue) {
                if (newLivesValue <= 0)
                    playerHasLost();
            }
        });



        hero = new HumanWizard(new vector(getLevelWidthInPx()/2,getLevelHeightInPx()/2), Teams.BLUE);
        hero.addLTL(new LivingThingListenerAdapter(){
            @Override
            public void onLevelUp(LivingThing lt) {
                if( lt.attributes.getLevel() == 2 )
                    lt.addAbility(new DamageBuff(lt, lt));
                mm.getUI().refreshSelectedUI();
            }
        });
        mm.add(hero);


        getBackground().setCenteredOn(hero.loc);


        //Setup exp adder
        final LivingThingListenerAdapter odl = new LivingThingListenerAdapter() {
            @Override
            public void onDeath(final LivingThing lt) {
                if( lt.getLastHurter() == hero )
                    hero.doOnYourThread(new Runnable() {
                        @Override
                        public void run() {
                            hero.addExp(lt.attributes.getExpGiven());
                        }
                });
            }
        };

        mm.getTeam(Teams.RED).getArmyManager().addListener(new ManagerListener<Humanoid>() {
            @Override
            public boolean onAdded(Humanoid h) {
                h.addLTL(odl);
                return false;
            }

            @Override
            public boolean onRemoved(Humanoid humanoid) {
                return false;
            }
        });


    }


    private long nextSpawn;

    @Override
    protected void subAct() {
        if( paused )
            return;
        getBackground().setCenteredOn(hero.loc);


        if( nextSpawn < GameTime.getTime() ){
            nextSpawn = GameTime.getTime() + 1000;

            try{
                Humanoid spawn = ForestSpawns.getSpawnClass(hero.attributes.getLevel()).getConstructor(vector.class, Teams.class).newInstance(getRandomLocOnMap(), Teams.RED);
                mm.add(spawn);
                spawn.aq.setFocusRangeSquared(Float.MAX_VALUE);
            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

            if( Math.random() < 0.8 ) {
                Humanoid ally = new HumanSoldier(getRandomLocOnMap(), Teams.BLUE);
                mm.add(ally);
                ally.aq.setFocusRangeSquared(Float.MAX_VALUE);
            }
        }

        if( Math.random() < 0.005){
            vector pickupLoc = new vector(getLevelWidthInPx() * Math.random(), getLevelHeightInPx() * Math.random());
            if( Math.random() < 0.3 ) {
                BuffPickup bp = new BuffPickup(pickupLoc, new Haste(null, null));
                pickups.add(bp);
                mm.getEm().add(bp.getAnim());
            }
            else{
                TripleAttackPickup bp = new TripleAttackPickup(pickupLoc);
                pickups.add(bp);
                mm.getEm().add(bp.getAnim());
            }
        }


        Iterator<Pickup> pIt = pickups.iterator();
        while( pIt.hasNext() ){
            Pickup p = pIt.next();
            if(p.isWithinRange(hero.loc) && p.canPickup(hero)) {
                p.pickedUp(mm, hero);
            }
            if( p.isOver() ) {
                pIt.remove();
                p.onOver();
            }
        }
    }

    private vector getRandomLocOnMap() {
        vector loc = new vector(getLevelWidthInPx()*Math.random(), getLevelHeightInPx()*Math.random());
        while(!mm.getCD().checkPlaceable(loc))
            loc.set(getLevelWidthInPx()*Math.random(), getLevelHeightInPx()*Math.random());

        return loc;
    }


    @Override
    protected void subOnStart() {
        super.subOnStart();
        ui.setSelected(hero);
    }



    @Override
    protected void createBorder() {
        //Create a border
        final int lvlWidthPx = getLevelWidthInPx();
        final int lvlHeightPx = getLevelHeightInPx();

        GameElement ge = getBorderElement(new vector());
        final int treeWidth = (int) ge.getStaticPerceivedArea().width();
        final int treeHeight = (int) ge.getStaticPerceivedArea().height();
        final int numHorz = lvlWidthPx/treeWidth;
        final int numVert = lvlHeightPx /treeHeight;


        //Top wall
        for( int i = 0 ; i < numHorz ; ++i )
            addDecoGE(getBorderElement(new vector(treeWidth / 2 + i * treeWidth, treeHeight / 2)));


        //Bottom wall of trees
        for( int i = 0 ; i < numHorz ; ++i )
            addDecoGE(getBorderElement(new vector((treeWidth / 2) + (i * treeWidth), lvlHeightPx - (treeHeight / 2))));


        //Left wall of trees
        for( int j = 0 ; j < numVert ; ++j )
            addDecoGE(getBorderElement(new vector((treeWidth / 2) , (treeHeight / 2) + (j * treeHeight))));


        //Right wall of trees
        for( int j = 0 ; j < numVert ; ++j )
            addDecoGE(getBorderElement(new vector(lvlWidthPx - (treeWidth / 2) , (treeHeight / 2) + (j * treeHeight))));
    }



    @NonNull
    protected GameElement getBorderElement(vector v){
        return new Tree(v);
    }

    protected void addDeco(vector loc, int drawable) {
        mm.getEm().add(new DecoAnimation(loc, drawable));
    }

    protected void addDecoGE(@NonNull GameElement ge) {
        ge.updateArea();
        mm.getGridUtil().setProperlyOnGrid(ge.area, Rpg.gridSize);
        GridUtil.getLocFromArea(ge.area, ge.getPerceivedArea(), ge.loc);

        boolean ¤‿¤ = false;
        for( RectF noBuildZone: noBuildZones )
            if( RectF.intersects(ge.area,noBuildZone) ){
                ¤‿¤ = true;
                break;
            }
        if( ¤‿¤ ) return;

        if (mm.getCD().checkPlaceable2(ge.area, false))
            mm.addGameElement(ge);
        else {
            Log.d(TAG, "Deco not placeable!");
        }
    }

    protected void addDecoGE(vector loc, @DrawableRes int drawableID) {
        GenericGameElement f = new GenericGameElement(loc,drawableID);
        addDecoGE(f);
    }


    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public long getPlayersScore() {
        return playersScore;
    }

    public Humanoid getHero() {
        return hero;
    }


    //*******************************   Abstract Methods   ***************************************//


    //****************************** End Abstract Methods   **************************************//







    //************************  Listener Methods And Interfaces   ********************************//

//    //Round Over Listener
//    protected final ArrayList<RoundOverListener> rols = new ArrayList<>();
//
//
//    public interface RoundOverListener{
//        void onRoundOver(int roundJustFinished);
//    }
//
//    public void addROL(RoundOverListener gol)		   		{	synchronized( rols ){	rols.add( gol );			}  	}
//    public boolean removeROL(RoundOverListener gol)		{	synchronized( rols ){	return rols.remove( gol );		}	}

}
