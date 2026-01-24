package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class StateManager {
    //TODO maybe change to dequeue/buffer in order to have a limit?
    private Stack<State> states_stack;
    private State cur;

    private Filter filter;

    private Stack<Link> link_stack;

    private class Link {
        public HashSet<String> reason_set;
        public int env;

        public Link(int env, String reason){
            this.reason_set = new HashSet<>();
            this.reason_set.add(reason);
            this.env = env;
        }
    }

    private HashMap<Integer, HashSet<Integer>> union_set;

    public StateManager(int currentEnv, Filter filter){
        this.union_set = new HashMap<>();
        for(Integer env : filter.getEnvNames().keySet()){
            this.union_set.put(env, new HashSet<>());
        }

        this.states_stack = new Stack<>();
        this.cur = new State(-1, true, currentEnv, filter, this.union_set);
        this.filter = filter;
        this.link_stack = new Stack<>();
    }

    public StateManager(int currentEnv, Filter filter, StateManager smanager){
        this.union_set = smanager.getUnionSet();
        this.union_set.get(smanager.getCurrent().getCurrentEnv()).addAll(smanager.getCurrent().getAvailable());

        this.states_stack = new Stack<>();
        this.cur = new State(-1, true, currentEnv, filter, this.union_set);
        this.filter = filter;
        this.link_stack = new Stack<>();   
    }

    public HashMap<Integer, HashSet<Integer>> getUnionSet(){
        return this.union_set;
    }

    public State getCurrent(){
        return this.cur;
    }

    public boolean isEnv() {
        return this.cur.getIsEnv();
    }

    public void addMetadataFilter(String element, String value){
        this.cur.addMetadataFilter(true, element, value);
    }

    public void removeMetadataFilter(String element, String value){
        this.cur.removeMetadataFilter(true, element, value);
    }

    public void addReferenceFilter(Integer env, String reason, String value){
        this.cur.addReferenceFilter(true, env, reason, value);
    }

    public void removeReferenceFilter(Integer env, String reason, String value){
        this.cur.removeReferenceFilter(true, env, reason, value);
    }

    public void addNotMetadataFilter(String element, String value){
        this.cur.addMetadataFilter(false, element, value);
    }

    public void removeNotMetadataFilter(String element, String value){
        this.cur.removeMetadataFilter(false, element, value);
    }

    public void addNotReferenceFilter(Integer env, String reason, String value){
        this.cur.addReferenceFilter(false, env, reason, value);
    }

    public void removeNotReferenceFilter(Integer env, String reason, String value){
        this.cur.removeReferenceFilter(false, env, reason, value);
    }

    public void link(int env, String reason) {
        this.link_stack.push(new Link(this.cur.getCurrentEnv(), reason));
        this.cur.link(env, reason);   
    }

    public void restore() {
        if(this.states_stack.size() > 1){
            this.states_stack.pop();
            this.cur = this.states_stack.pop();
        }
    }

    public void goback() {
        if(!this.link_stack.isEmpty()){
            Link last_link = this.link_stack.pop();
            for(String reason : last_link.reason_set){ //TODO this for loop is just a patch
                this.cur.link(last_link.env, reason);
            }   
        }
    }

    public void save() {
        this.states_stack.push(new State(this.cur, this.filter, this.union_set));
    }

    public void selectEnv() {
        this.cur.setIsEnv(true);
    }

    public void selectEnt(Integer ent_id){
        this.cur.setIsEnv(false);
        this.cur.setEnt(ent_id);
    }
}
