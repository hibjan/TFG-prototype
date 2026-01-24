package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Info {
    // ANSI escape codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String ITALIC = "\u001B[3m";
    private static final String UNDERLINE = "\u001B[4m";

    private Filter filter; 

    public Info(Filter filter){
        this.filter = filter;
    }

    private void printColumns(State state){
        int colWidth = filter.getColWidth(state.getCurrentEnv());
        int rows = 0;
        HashMap<String, SortedMap<String, SortedSet<String>>> columns = new HashMap<>();

        //METADATA
        columns.put("m", new TreeMap<>());
        for(String column : filter.getMetadata(state.getCurrentEnv()).keySet()){
            for(String value : filter.getMetadata(state.getCurrentEnv()).get(column).keySet()){
                HashSet<Integer> set = state.getAvailable();
                set.retainAll(filter.getMetadataSet(state.getCurrentEnv(), column, value));
                if(!set.isEmpty() || (set.isEmpty() && state.containsNotMetadataFilter(column, value))){
                    if(!columns.get("m").containsKey(column)){
                        columns.get("m").put(column, new TreeSet<>());
                    }
                    if(state.containsMetadataFilter(column, value)){
                        value = "(*) " + value;
                    }
                    else if(state.containsNotMetadataFilter(column, value)){
                        value = "(!) " + value;
                    }
                    int old_column_size = columns.get("m").get(column).size();
                    columns.get("m").get(column).add(value);
                    int new_column_size = columns.get("m").get(column).size();
                    if(new_column_size > old_column_size){
                        rows = Math.max(rows, new_column_size);
                    }
                }
            }
        }
           
        //REFERENCES && LINKS
        columns.put("r", new TreeMap<>());
        columns.put("l", new TreeMap<>());
        columns.get("l").put("Links", new TreeSet<>());
        for(Integer ent : state.getAvailable()){
            for(Integer env : filter.getReferences(state.getCurrentEnv(), ent).keySet()){
                for(String reason : filter.getReferences(state.getCurrentEnv(), ent).get(env).keySet()){
                    for(Integer reference_ent : filter.getReferenceSet(state.getCurrentEnv(), env, reason, ent.toString())){
                        // REFERENCES
                        String column = reason + " (" + env + ")";
                        if(!columns.get("r").containsKey(column)){
                            columns.get("r").put(column, new TreeSet<>());
                        }
                        String value = filter.getEntName(env, reference_ent) + " (" + reference_ent.toString() + ")";
                        if(state.containsReferenceFilter(env, reason, reference_ent)){
                            value = "(*) " + value;
                        }
                        int old_column_size = columns.get("r").get(column).size();
                        columns.get("r").get(column).add(value);
                        int new_column_size = columns.get("r").get(column).size();
                        if(new_column_size > old_column_size){
                            rows = Math.max(rows, new_column_size);
                        }
                        // LINKS
                        if(state.getAvailable(env).contains(reference_ent)){
                            columns.get("l").get("Links").add(reason + " (" + env + ")");
                        }
                    }
                }
            }
        }
        for(Integer env : state.getNotReferenceFilter().keySet()){
            for(String reason : state.getNotReferenceFilter().get(env).keySet()){
                for(String reference_ent : state.getNotReferenceFilter().get(env).get(reason)){
                    String column = reason + " (" + env + ")";
                    if(!columns.get("r").containsKey(column)){
                        columns.get("r").put(column, new TreeSet<>());
                    }
                    String value = "(!) " + filter.getEntName(env, Integer.parseInt(reference_ent)) + " (" + reference_ent + ")";
                    int old_column_size = columns.get("r").get(column).size();
                    columns.get("r").get(column).add(value);
                    int new_column_size = columns.get("r").get(column).size();
                    if(new_column_size > old_column_size){
                        rows = Math.max(rows, new_column_size);
                    }
                }
            }
        }
        /* 
        for(){
            
            String value = filter.getEntName(env, reference_ent) + " (" + reference_ent.toString() + ")";
            else if(state.containsNotReferenceFilter(env, reason, reference_ent)){
                            value = "(!) " + value;
                        }
        }
                        */
    
        StringBuilder sb = new StringBuilder();
        StringBuilder sb_line = new StringBuilder();
        for(String type : columns.keySet()){
            for(String column : columns.get(type).keySet()){
                sb.append(String.format("| %-" + colWidth + "s ", column));
                sb_line.append("+").append("-".repeat(colWidth + 2));
            }
        }

        sb.append(" |");
        sb_line.append("+");
        String header = sb.toString();
        String line = sb_line.toString();

        System.out.println(line);
        System.out.println(header);
        System.out.println(line);

        for (int i = 0; i < rows; i++) {
            StringBuilder sb_row = new StringBuilder();
            for(String type : columns.keySet()){
                for(String column : columns.get(type).keySet()){
                   int j = 0;
                    String col_name = "";
                    for (String val : columns.get(type).get(column)) {
                        if (j == i) {
                            col_name = val;
                            break;
                        }
                        j++;
                    }
                    sb_row.append(String.format("| %-" + colWidth + "s ", col_name));
                }
            }
            sb_row.append(" |");
            String row = sb_row.toString();
            System.out.println(row);
        }

        // Bottom line
        System.out.println(line);
        System.out.println();
    }

    public void printEntities(State state){
        printEnv(state.getCurrentEnv());
        printColumns(state);

        for(Integer id : state.getAvailable()){
            System.out.println(filter.getEntName(state.getCurrentEnv(), id) + " (" + id + ")");
        }
        System.out.println("\n------\n");
        for(Integer env : state.getUnionSet().keySet()){
            for(Integer id : state.getUnionSet().get(env)){
                System.out.println(filter.getEnvNames().get(env) + " (" + env + ")" + " - " + filter.getEntName(env, id) + " (" + id + ")");
            }
        }
    }

    public void printEntity(State state) {

        printEnv(state.getCurrentEnv());

        //CONTENTS
        for(String content : filter.getEntContents(state.getCurrentEnv(), state.getCurrentEnt()).keySet()){
            String value = filter.getEntContents(state.getCurrentEnv(), state.getCurrentEnt()).get(content);
            System.out.println(UNDERLINE + content + RESET + ": " + value);
            System.out.println();
        }


        /* 

        //CONTENTS
        for(String content : contents_map.get(currentEnv).get(id).keySet()){
            String value = contents_map.get(currentEnv).get(id).get(content);
            System.out.println(UNDERLINE + content + RESET + ": " + value);
            System.out.println();
        }

        //METADATA
        for(String tag : metadata.get(currentEnv).get(id).keySet()){
            System.out.println(UNDERLINE + tag + RESET + ":");
            for(String value : metadata.get(currentEnv).get(id).get(tag)){
                System.out.println("   - " + value);
                System.out.println();
            }
        }

        //REFERENCES
        for(Integer reference_env : references_map.get(currentEnv).get(id).keySet()){
            System.out.println(BOLD + UNDERLINE + envs.get(reference_env) + " (" + reference_env + ")" + RESET + ":");
            for(String reason : references_map.get(currentEnv).get(id).get(reference_env).keySet()){
                System.out.println(reason + ":");
                for(Integer reference_id : references_map.get(currentEnv).get(id).get(reference_env).get(reason)){
                    System.out.println("    - " + contents_map.get(reference_env).get(reference_id).get("name") + " (" + reference_id + ")");
                }
            }
            System.out.println();
        }
        */
    }

    public void printEnvs(){
        printEnv(-1);
    }

    private void printEnv(int env){
        System.out.println();

        StringBuilder sb_env = new StringBuilder();
        sb_env.append("[");
        boolean first = true;
        for(Integer environ : this.filter.getEnvNames().keySet()){
            String name = this.filter.getEnvNames().get(environ);
            sb_env.append(first ? " " : ", ").append(environ == env ? BOLD + UNDERLINE + name + " (" + environ + ")" + RESET : name + " (" + environ + ")");
            if(first){
                first = false;
            }
        }
        sb_env.append(" ]");
        System.out.println(sb_env);
        System.out.println();
    }
}