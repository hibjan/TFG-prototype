package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.SortedSet;

public class State {
    private boolean isEnv;
    private int current_env;
    private int current_ent;
    // Type (True -> regular, False -> not) ->
    // ENV (1=Movies) ->
    // Filter (Genre) ->
    // Values (Action, Drama, ...)
    private HashMap<Boolean, HashMap<Integer, HashMap<String, HashSet<String>>>> mfilters;
    // Type (True -> regular, False -> not) ->
    // ENV (1=Movies) ->
    // REFERENCE_ENV (2=People) ->
    // Filter (Director) ->
    // Values (108, 350, ...)
    private HashMap<Boolean, HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<String>>>>> rfilters;
    private HashMap<Integer, HashSet<Integer>> filter_set;
    private HashSet<Integer> link_set;

    private HashMap<Integer, HashSet<Integer>> union_set;

    private Filter filter;

    public State(int current_ent, boolean isEnv, int current_env, Filter filter, HashMap<Integer, HashSet<Integer>> union_set){
        this.isEnv = isEnv;
        this.current_env = current_env;
        this.current_ent = current_ent;

        this.filter_set = new HashMap<>();
        for(Integer env : filter.getEnvNames().keySet()){
            this.filter_set.put(env, new HashSet<>());
        }
        this.link_set = new HashSet<>();

        this.mfilters = new HashMap<>();
        this.rfilters = new HashMap<>();

        this.union_set = union_set;

        this.filter = filter;
    }

    public State(State state, Filter filter, HashMap<Integer, HashSet<Integer>> union_set){
        this.isEnv = state.getIsEnv();
        this.current_env = state.getCurrentEnv();
        this.current_ent = state.getCurrentEnt();

        this.filter_set = state.fsetDeepCopy();
        this.link_set = state.lsetDeepCopy();

        this.mfilters = state.mfiltersDeepCopy();
        this.rfilters = state.rfiltersDeepCopy();

        this.union_set = union_set;

        this.filter = filter;
    }

    public boolean getIsEnv() {
        return this.isEnv;
    }

    public int getCurrentEnv() {
        return this.current_env;
    }

    public int getCurrentEnt() {
        return this.current_ent;
    }

    public HashMap<Boolean, HashMap<Integer, HashMap<String, HashSet<String>>>> mfiltersDeepCopy(){
        HashMap<Boolean, HashMap<Integer, HashMap<String, HashSet<String>>>> mfilters_copy = new HashMap<>();
        for(Boolean type : this.mfilters.keySet()){
            mfilters_copy.put(type, new HashMap<>());
            for(Integer env : this.mfilters.get(type).keySet()){
                mfilters_copy.get(type).put(env, new HashMap<>());
                for(String attribute : this.mfilters.get(type).get(env).keySet()){
                    mfilters_copy.get(type).get(env).put(attribute, new HashSet<>(this.mfilters.get(type).get(env).get(attribute)));
                }
            }
        }
        
        return mfilters_copy;
    }

    public HashMap<Boolean, HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<String>>>>> rfiltersDeepCopy(){
        HashMap<Boolean, HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<String>>>>> rfilters_copy = new HashMap<>();
        for(Boolean type : this.rfilters.keySet()){
            rfilters_copy.put(type, new HashMap<>());
            for(Integer env : this.rfilters.get(type).keySet()){
                rfilters_copy.get(type).put(env, new HashMap<>());
                for(Integer reference_env : this.rfilters.get(type).get(env).keySet()){
                    rfilters_copy.get(type).get(env).put(reference_env, new HashMap<>());
                    for(String reason : this.rfilters.get(type).get(env).get(reference_env).keySet()){
                        rfilters_copy.get(type).get(env).get(reference_env).put(reason, new HashSet<>(this.rfilters.get(type).get(env).get(reference_env).get(reason)));
                    }
                }   
            }
        }
        
        return rfilters_copy;
    }

    public HashMap<Integer, HashSet<Integer>> fsetDeepCopy(){
        HashMap<Integer, HashSet<Integer>> fset_copy = new HashMap<>();
        for(Integer env : this.filter_set.keySet()){
            fset_copy.put(env, new HashSet<>(this.filter_set.get(env)));
        }
        return fset_copy;
    }

    public HashSet<Integer> lsetDeepCopy(){
        return new HashSet<>(this.link_set);
    }

    public void addMetadataFilter(boolean type, String element, String value){
        //Add to filter data structure
        if(!mfilters.containsKey(type)){
            mfilters.put(type, new HashMap<>());
        }
        if(!mfilters.get(type).containsKey(current_env)){
            mfilters.get(type).put(current_env, new HashMap<>());
        }
        if(!mfilters.get(type).get(current_env).containsKey(element)){
            mfilters.get(type).get(current_env).put(element, new HashSet<>());
        }
        mfilters.get(type).get(current_env).get(element).add(value);
        
        intersectMetadataFilter(type, element, value);
    }

    private void intersectMetadataFilter(boolean type, String element, String value){
        //Intersect with current set
        HashSet<Integer> set = new HashSet<>();
        if(type){
            set = filter.getMetadataSet(this.current_env, element, value);
        }
        else {
            set = filter.getNotMetadataSet(this.current_env, element, value);
        }

        if(this.filter_set.get(current_env).isEmpty()){
            this.filter_set.put(current_env, new HashSet<>(set));
        }
        else {
            this.filter_set.get(current_env).retainAll(set);
        }
    }

    public void removeMetadataFilter(boolean type, String element, String value){
        if(mfilters.containsKey(type) &&
        mfilters.get(type).containsKey(current_env) &&
        mfilters.get(type).get(current_env).containsKey(element)){
            mfilters.get(type).get(current_env).get(element).remove(value);
            if(mfilters.get(type).get(current_env).get(element).isEmpty()){
                mfilters.get(type).get(current_env).remove(element);
                if(mfilters.get(type).get(current_env).isEmpty()){
                    mfilters.get(type).remove(current_env);
                    if(mfilters.get(type).isEmpty()){
                        mfilters.remove(type);
                    }
                }
            }
        }

        regenerateFilters();
    }

    public boolean containsMetadataFilter(String attribute, String value) {
        return containsMetadataFilter(true, attribute, value);
    }

    public boolean containsNotMetadataFilter(String attribute, String value) {
        return containsMetadataFilter(false, attribute, value);
    }

    private boolean containsMetadataFilter(boolean type, String attribute, String value){
        return mfilters.containsKey(type) &&
            mfilters.get(type).containsKey(current_env) && 
            mfilters.get(type).get(current_env).containsKey(attribute) && 
            mfilters.get(type).get(current_env).get(attribute).contains(value);
    }

    public void addReferenceFilter(boolean type, Integer env, String reason, String value) {
        //Add to filter data structure
        if(!rfilters.containsKey(type)){
            rfilters.put(type, new HashMap<>());
        }
        if(!rfilters.get(type).containsKey(current_env)){
            rfilters.get(type).put(current_env, new HashMap<>());
        }
        if(!rfilters.get(type).get(current_env).containsKey(env)){
            rfilters.get(type).get(current_env).put(env, new HashMap<>());
        }
        if(!rfilters.get(type).get(current_env).get(env).containsKey(reason)){
            rfilters.get(type).get(current_env).get(env).put(reason, new HashSet<>());
        }
        rfilters.get(type).get(current_env).get(env).get(reason).add(value);
        
        intersectReferenceFilter(type, env, reason, value);
    }

    private void intersectReferenceFilter(boolean type, Integer env, String reason, String value){
        //Intersect with current set
        HashSet<Integer> set = new HashSet<>();
        if(type){
            set = filter.getReferenceSetInv(current_env, env, reason, value);
        }
        else {
            set = filter.getNotReferenceSetInv(current_env, env, reason, value);
        }

        if(this.filter_set.get(current_env).isEmpty()){
            this.filter_set.put(current_env, new HashSet<>(set));
        }
        else {
            this.filter_set.get(current_env).retainAll(set);
        }
    }

    public void removeReferenceFilter(boolean type, Integer env, String reason, String value) {
        if(rfilters.containsKey(type) &&
        rfilters.get(type).containsKey(current_env) &&
        rfilters.get(type).get(current_env).containsKey(env) &&
        rfilters.get(type).get(current_env).get(env).containsKey(reason) &&
        rfilters.get(type).get(current_env).get(env).get(reason).contains(value)){
            rfilters.get(type).get(current_env).get(env).get(reason).remove(value);
            if(rfilters.get(type).get(current_env).get(env).get(reason).isEmpty()){
                rfilters.get(type).get(current_env).get(env).remove(reason);
                if(rfilters.get(type).get(current_env).get(env).isEmpty()){
                    rfilters.get(type).get(current_env).remove(env);
                    if(rfilters.get(type).get(current_env).isEmpty()){
                        rfilters.get(type).remove(current_env);
                        if(rfilters.get(type).isEmpty()){
                            rfilters.remove(type);
                        }
                    }
                }
            }
        }

        regenerateFilters();
    }

    public boolean containsReferenceFilter(Integer env, String reason, Integer reference_ent) {
        return containsReferenceFilter(true, env, reason, reference_ent);
    }

    public  HashMap<Integer, HashMap<String, HashSet<String>>> getNotReferenceFilter(){
        if(this.rfilters.containsKey(false) && this.rfilters.get(false).containsKey(current_env)){
            return this.rfilters.get(false).get(current_env);
        }
        return new HashMap<>();
    }
    public boolean containsNotReferenceFilter(Integer env, String reason, Integer reference_ent) {
        return containsReferenceFilter(false, env, reason, reference_ent);
    }

    private boolean containsReferenceFilter(boolean type, Integer env, String reason, Integer reference_ent) {
        return rfilters.containsKey(type) &&
            rfilters.get(type).containsKey(current_env) && 
            rfilters.get(type).get(current_env).containsKey(env) && 
            rfilters.get(type).get(current_env).get(env).containsKey(reason) && 
            rfilters.get(type).get(current_env).get(env).get(reason).contains(reference_ent.toString());
    }

    private void regenerateFilters() {
        this.filter_set.put(current_env, new HashSet<>());

        //METADATA
        for(Boolean type : mfilters.keySet()){
            if(mfilters.get(type).containsKey(current_env)){
                for(String attribute : mfilters.get(type).get(current_env).keySet()){
                    for(String attribute_value : mfilters.get(type).get(current_env).get(attribute)){
                        intersectMetadataFilter(type, attribute, attribute_value);
                    }
                }
            }
        }
        

        //REFERENCES
        for(Boolean type : rfilters.keySet()){
            if(rfilters.get(type).containsKey(current_env)){
                for(Integer env : rfilters.get(type).get(current_env).keySet()){
                    for(String reason : rfilters.get(type).get(current_env).get(env).keySet()){
                        for(String value : rfilters.get(type).get(current_env).get(env).get(reason)){
                            intersectReferenceFilter(type, env, reason, value);
                        }
                    }
                }
            }
        }   
    }

    public HashSet<Integer> getAvailable() {
        HashSet<Integer> result;
        
        if(!this.filter_set.get(current_env).isEmpty() && !this.link_set.isEmpty()){ //BOTH WITH CONTENTS
            result = new HashSet<>(this.link_set);
            result.retainAll(this.filter_set.get(current_env));
        }
        else if(!this.filter_set.get(current_env).isEmpty()){ //ONLY FILTER
            result = this.filter_set.get(current_env);
        }
        else if(!this.link_set.isEmpty()){ //ONLY LINK
            result = this.link_set;
        }
        else {
            return new HashSet<>(filter.getEnvEnts(current_env));
        }
        return new HashSet<>(result);
    }

    public HashSet<Integer> getAvailable(int env) {
        if(!this.filter_set.get(env).isEmpty()){
            return new HashSet<>(this.filter_set.get(env));
        }
        return new HashSet<>(filter.getEnvEnts(env));
    }

    public void link(int env, String reason) {
        HashSet<Integer> available = getAvailable();
        
        this.link_set = new HashSet<>();
        for(Integer ent : available){
            this.link_set.addAll(filter.getReferenceSet(current_env, env, reason, ent.toString()));
        }

        this.current_env = env;
    }

    public HashMap<Integer, HashSet<Integer>> getUnionSet() {
        return this.union_set;
    }

    public void setIsEnv(boolean isEnv) {
        this.isEnv = isEnv;
    }

    public void setEnt(Integer entId) {
        this.current_ent = entId;
    }
}
