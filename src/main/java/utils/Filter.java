package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

public class Filter {
    // ENV (1=Movies) ->
    // Entity ID (1="X") ->
    // ENV Reference ID (2=People) ->
    // Reason ("Actor") ->
    // Set of IDs from ENV Reference (Actors referenced by X)
    private HashMap<Integer, HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<Integer>>>>> references_map;
    // ENV (1=Movies) ->
    // ENV Reference ID (2=People) ->
    // Reason ("Actor") ->
    // Set of IDs from ENV Reference (Actors)
    private HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<Integer>>>> references_reason_map;
    // ENV (1=Movies) ->
    // Entity ID (1="X) ->
    // Field name ->
    // Content
    private HashMap<Integer, HashMap<Integer, HashMap<String, String>>> contents_map;
    // ENV (1=Movies) ->
    // Filter Name (Genres) ->
    // Filter Value (Action) ->
    // Set of IDs from ENV (Movies with Action Genre)
    private HashMap<Integer, HashMap<String, HashMap<String, HashSet<Integer>>>> metadata_map;


    // ENV (1=Movies) ->
    // Entity ID (1="X) ->
    // Field name ->
    // Set of values
    private HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<String>>>> metadata;
    // ENV (1=Movies) ->
    // Column ->
    // Set of ID-Name
    private HashMap<Integer, HashMap<String, SortedSet<String>>> columns;
    // ENV (1=Movies) ->
    // Column ->
    // Type (m / r)
    private HashMap<Integer, HashMap<String, String>> column_type;
    // ENV (1=Movies) ->
    // Number of rows the table should have
    private HashMap<Integer, Integer> rows;
    // ENV (1=Movies) ->
    // Width of the columns the table should have
    private HashMap<Integer, Integer> column_width;
    // ENV (1=Movies) ->
    // Name of the environment
    private HashMap<Integer, String> envs;
    // ENV (1=Movies) ->
    // All of the entities (1,2,...)
    private HashMap<Integer, HashSet<Integer>> ents;

    public Filter(JSONObject dataset){
        this.contents_map = new HashMap<>();
        this.metadata_map = new HashMap<>();
        this.references_map = new HashMap<>();
        this.references_reason_map = new HashMap<>();

        this.metadata = new HashMap<>();
        this.columns = new HashMap<>();
        this.column_type = new HashMap<>();
        this.rows = new HashMap<>();
        this.column_width = new HashMap<>();
        this.envs = new HashMap<>();

        this.ents = new HashMap<>();

        this.processJSON(dataset);
    }

    private void processJSON(JSONObject dataset) {
        JSONArray collections = dataset.getJSONArray("collections");

        for(int i = 0; i < collections.length(); i++){
            JSONObject collection = (JSONObject) collections.get(i);

            contents_map.put((Integer) collection.get("id"), new HashMap<>());
            metadata_map.put((Integer) collection.get("id"), new HashMap<>());
            metadata.put((Integer) collection.get("id"), new HashMap<>());
            references_map.put((Integer) collection.get("id"), new HashMap<>());
            references_reason_map.put((Integer) collection.get("id"), new HashMap<>());

            columns.put((Integer) collection.get("id"), new HashMap<>());
            column_type.put((Integer) collection.get("id"), new HashMap<>());
            rows.put((Integer) collection.get("id"), 0);
            column_width.put((Integer) collection.get("id"), 0);
            envs.put((Integer) collection.get("id"), (String) collection.get("name"));

            ents.put((Integer) collection.get("id"), new HashSet<>());
        }

        JSONArray objects = dataset.getJSONArray("objects");

        for(int i = 0; i < objects.length(); i++){
            JSONObject object = (JSONObject) objects.get(i);

            ents.get((Integer) object.get("collection_id")).add((Integer) object.get("id"));

            //CONTENTS
            contents_map.get((Integer) object.get("collection_id")).put((Integer) object.get("id"), new HashMap<>());

            JSONObject contents = (JSONObject) object.get("contents");

            for (String key : contents.keySet()) {
                contents_map.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).put(key, (String) contents.get(key));
            }

            //METADATA
            JSONObject metadata = (JSONObject) object.get("metadata");

            this.metadata.get((Integer) object.get("collection_id")).put((Integer) object.get("id"), new HashMap<>());

            for (String key : metadata.keySet()) {
                this.metadata.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).put(key, new HashSet<>());

                if(!metadata_map.get((Integer) object.get("collection_id")).containsKey(key)) {
                    metadata_map.get((Integer) object.get("collection_id")).put(key, new HashMap<>());
                }

                //column
                if(!columns.get((Integer) object.get("collection_id")).containsKey(key)) {
                    columns.get((Integer) object.get("collection_id")).put(key, new TreeSet<>());
                    column_type.get((Integer) object.get("collection_id")).put(key, "m");
                    column_width.put((Integer) object.get("collection_id"), Math.max(column_width.get((Integer) object.get("collection_id")), key.length() + 2));
                }
                //column

                JSONArray metadata_values = (JSONArray) metadata.get(key);

                for(int j = 0; j < metadata_values.length(); j++){
                    this.metadata.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).get(key).add((String) metadata_values.get(j));

                    if(!metadata_map.get((Integer) object.get("collection_id")).get(key).containsKey((String) metadata_values.get(j))) {
                        metadata_map.get((Integer) object.get("collection_id")).get(key).put((String) metadata_values.get(j), new HashSet<>());
                    }
                    metadata_map.get((Integer) object.get("collection_id")).get(key).get((String) metadata_values.get(j)).add((Integer) object.get("id"));

                    //column
                    int old_column_size = columns.get((Integer) object.get("collection_id")).get(key).size();
                    columns.get((Integer) object.get("collection_id")).get(key).add((String) metadata_values.get(j));
                    int new_column_size = columns.get((Integer) object.get("collection_id")).get(key).size();
                    if(new_column_size > old_column_size){
                        rows.put((Integer) object.get("collection_id"), Math.max(rows.get((Integer) object.get("collection_id")), new_column_size));
                        column_width.put((Integer) object.get("collection_id"), Math.max(column_width.get((Integer) object.get("collection_id")), metadata_values.get(j).toString().length() + 2));
                    }
                    //column
                }
            }

            //REFERENCES
            JSONArray references = (JSONArray) object.get("references");

            for(int j = 0; j < references.length(); j++) {
                JSONObject reference = (JSONObject) references.get(j);

                if (!references_map.get((Integer) object.get("collection_id")).containsKey((Integer) object.get("id"))) {
                    references_map.get((Integer) object.get("collection_id")).put((Integer) object.get("id"), new HashMap<>());
                }

                if (!references_map.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).containsKey((Integer) reference.get("reference_collection_id"))) {
                    references_map.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).put((Integer) reference.get("reference_collection_id"), new HashMap<>());
                }

                if (!references_map.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).get((Integer) reference.get("reference_collection_id")).containsKey((String) reference.get("reason"))) {
                    references_map.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).get((Integer) reference.get("reference_collection_id")).put((String) reference.get("reason"), new HashSet<>());
                }

                references_map.get((Integer) object.get("collection_id")).get((Integer) object.get("id")).get((Integer) reference.get("reference_collection_id")).get((String) reference.get("reason")).add((Integer) reference.get("reference_id"));

                //Reason map

                if (!references_reason_map.get((Integer) object.get("collection_id")).containsKey((Integer) reference.get("reference_collection_id"))) {
                    references_reason_map.get((Integer) object.get("collection_id")).put((Integer) reference.get("reference_collection_id"), new HashMap<>());
                }

                if (!references_reason_map.get((Integer) object.get("collection_id")).get((Integer) reference.get("reference_collection_id")).containsKey((String) reference.get("reason"))) {
                    references_reason_map.get((Integer) object.get("collection_id")).get((Integer) reference.get("reference_collection_id")).put((String) reference.get("reason"), new HashSet<>());
                }

                references_reason_map.get((Integer) object.get("collection_id")).get((Integer) reference.get("reference_collection_id")).get((String) reference.get("reason")).add((Integer) reference.get("reference_id"));


            }
        }

        //Get reference columns
        for(int i = 0; i < objects.length(); i++){
            JSONObject object = (JSONObject) objects.get(i);

            //REFERENCES
            JSONArray references = (JSONArray) object.get("references");

            for(int j = 0; j < references.length(); j++) {
                JSONObject reference = (JSONObject) references.get(j);

                //column
                String column_name = reference.get("reason").toString() + " (" + reference.get("reference_collection_id").toString() + ")";
                if(!columns.get((Integer) object.get("collection_id")).containsKey(column_name)) {
                    columns.get((Integer) object.get("collection_id")).put(column_name, new TreeSet<>());
                    column_type.get((Integer) object.get("collection_id")).put((String) reference.get("reason"), "r");
                    column_width.put((Integer) object.get("collection_id"), Math.max(column_width.get((Integer) object.get("collection_id")), reference.get("reason").toString().length() + 2));
                }
                String name = contents_map.get((Integer) reference.get("reference_collection_id")).get((Integer) reference.get("reference_id")).get("name");
                String reference_name = name + " (" + reference.get("reference_id").toString() + ")";
                int old_column_size = columns.get((Integer) object.get("collection_id")).get(column_name).size();
                columns.get((Integer) object.get("collection_id")).get(column_name).add(reference_name);
                int new_column_size = columns.get((Integer) object.get("collection_id")).get(column_name).size();
                if(new_column_size > old_column_size){
                    rows.put((Integer) object.get("collection_id"), Math.max(rows.get((Integer) object.get("collection_id")), new_column_size));
                    column_width.put((Integer) object.get("collection_id"), Math.max(column_width.get((Integer) object.get("collection_id")), reference_name.length() + 2));
                }

                //Reason
                if(!columns.get((Integer) object.get("collection_id")).containsKey("Reason")) {
                    columns.get((Integer) object.get("collection_id")).put("Reason", new TreeSet<>());
                    column_type.get((Integer) object.get("collection_id")).put("Reason", "x");
                    column_width.put((Integer) object.get("collection_id"), Math.max(column_width.get((Integer) object.get("collection_id")), 6));
                }

                String reason_name = (String) reference.get("reason") + " (" + reference.get("reference_collection_id").toString() + ")";
                old_column_size = columns.get((Integer) object.get("collection_id")).get("Reason").size();
                columns.get((Integer) object.get("collection_id")).get("Reason").add(reason_name);
                new_column_size = columns.get((Integer) object.get("collection_id")).get("Reason").size();
                if(new_column_size > old_column_size){
                    rows.put((Integer) object.get("collection_id"), Math.max(rows.get((Integer) object.get("collection_id")), new_column_size));
                    column_width.put((Integer) object.get("collection_id"), Math.max(column_width.get((Integer) object.get("collection_id")), reason_name.length() + 2));
                }

                //column
            }
        }
    }

    public HashSet<Integer> getMetadataSet(int currentEnv, String element, String value) {
        return new HashSet<>(this.metadata_map.get(currentEnv).get(element).get(value));
    }

    public HashSet<Integer> getNotMetadataSet(int currentEnv, String element, String value) {
        HashSet<Integer> set = new HashSet<>(ents.get(currentEnv));
        set.removeAll(this.metadata_map.get(currentEnv).get(element).get(value));
        return new HashSet<>(set);
    }

    public HashMap<Integer, String> getEnvNames() {
        return new HashMap<>(this.envs);
    }

    public String getEntName(int env, Integer id) {
        return new String(contents_map.get(env).get(id).get("name"));
    }

    public HashSet<Integer> getEnvEnts(int env) {
        return new HashSet<>(ents.get(env));
    }

    public HashMap<String, HashMap<String, HashSet<Integer>>> getMetadata(int currentEnv) {
        return this.metadata_map.get(currentEnv);
    }

    public int getColWidth(int currentEnv) {
        return this.column_width.get(currentEnv);
    }

    //Get all of the entities from current_env that are connected to value with reason x
    public HashSet<Integer> getReferenceSetInv(int current_env, Integer env, String reason, String value) {
        return new HashSet<>(this.references_map.get(env).get(Integer.parseInt(value)).get(current_env).get(reason));
    }

    public HashSet<Integer> getNotReferenceSetInv(int current_env, Integer env, String reason, String value) {
        HashSet<Integer> set = new HashSet<>(ents.get(current_env));
        set.removeAll(this.references_map.get(env).get(Integer.parseInt(value)).get(current_env).get(reason));
        return new HashSet<>(set);
    }

    //Get all of the entities from env that are connected to value with reason x
    public HashSet<Integer> getReferenceSet(int current_env, Integer env, String reason, String value) {
        if(!this.references_map.containsKey(current_env) ||
            !this.references_map.get(current_env).containsKey(Integer.parseInt(value)) ||
            !this.references_map.get(current_env).get(Integer.parseInt(value)).containsKey(env) ||
            !this.references_map.get(current_env).get(Integer.parseInt(value)).get(env).containsKey(reason)){
            return new HashSet<>();
        }
        return new HashSet<>(this.references_map.get(current_env).get(Integer.parseInt(value)).get(env).get(reason));
    }

    public HashMap<Integer, HashMap<String, HashSet<Integer>>> getReferences(int currentEnv, int ent) {
        return this.references_map.get(currentEnv).get(ent);
    }

    public HashMap<String, String> getEntContents(int env, int id){
        return this.contents_map.get(env).get(id);
    }
}
