package main;

import java.io.*;
import org.json.JSONObject;
import utils.Filter;
import utils.Info;
import utils.StateManager;

/*
-----------------------------------------------------------------------------------------------------------------------
Commands of navigation:
-----------------------------------------------------------------------------------------------------------------------

add_mfilter [-n] tag=value          -> adds metadata filter (Genre=Action)

rm_mfilter [-n] tag=value           -> removes metadata filter (Genre=Action)

add_rfilter [-n] env->tag=value     -> adds reference filter (2->Director=108)

rm_rfilter [-n] env->tag=value      -> removes reference filter (2->Director=108)

link env-reason                     -> changes to an environment using a specific reason (env=2, reason=Director)

union                               -> stores entities in current_env (union operation)

restore                             -> restores last state (step)

goback                              -> goes to the last environment viewed using the reason with which it was entered

select ent=value                    -> shows entity details (ent=122)

select env                          -> show environment entities

exit                                -> stops program





follow env->id              -> follow entity details (1->122)



-----------------------------------------------------------------------------------------------------------------------
*/

public class Main {

    public static void main(String[] args) {
        boolean error = false;

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        Filter filter = new Filter(loadJSON());

        Info info = new Info(filter);

        StateManager smanager = new StateManager(selectEnv(info, reader), filter);

        while(true) {
            if(error) {
                error = false;
            }
            else {
                smanager.save();
                if(smanager.isEnv()) {
                    info.printEntities(smanager.getCurrent());
                } 
                else {
                    info.printEntity(smanager.getCurrent());
                }
            }

            System.out.print("\nEnter command: ");
            String input;
            try {
                input = reader.readLine();
                if (input == null) break; // end of stream (Ctrl+D)

                input = input.trim();

                // Split command into keyword + args
                String[] parts = input.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String argument = (parts.length > 1) ? parts[1] : "";
                String[] argument_parts;
                String element;
                String value;
                String type;
                int int_value;
                boolean back = false;
                boolean not_filter;

                switch (command) {
                    case "exit":
                        return;

                    case "select":
                        argument_parts = argument.split("=", 2);
                        type = argument_parts[0];
                        switch(type){
                            case "env":
                                smanager.selectEnv();
                                break;
                            case "ent":
                                smanager.selectEnt(Integer.parseInt(argument_parts[1]));
                                break;
                            default:
                                System.out.println("Error: invalid argument");
                                break;
                        }
                        break;

                        /*
                    case "follow":
                        argument_parts = argument.split("->", 2);
                        int new_env = Integer.parseInt(argument_parts[0]);
                        int id = Integer.parseInt(argument_parts[1]);
                        current_env = new_env;
                        current_ent = id;
                        filter.newEnv(current_env);
                        break;
                    */

                    case "add_mfilter":
                        argument_parts = argument.split("\\s+", 2);
                        not_filter = argument_parts.length == 2 && argument_parts[0].equals("-n");
                        argument_parts = argument_parts[argument_parts.length - 1].split("=", 2);
                        element = argument_parts[0];
                        value = argument_parts[1];
                        if(not_filter){
                            smanager.addNotMetadataFilter(element, value);
                        }
                        else {
                            smanager.addMetadataFilter(element, value);
                        }
                        break;

                    case "rm_mfilter":
                        argument_parts = argument.split("\\s+", 2);
                        not_filter = argument_parts.length == 2 && argument_parts[0].equals("-n");
                        argument_parts = argument_parts[argument_parts.length - 1].split("=", 2);
                        element = argument_parts[0];
                        value = argument_parts[1];
                        if(not_filter){
                            smanager.removeNotMetadataFilter(element, value);
                        }
                        else {
                            smanager.removeMetadataFilter(element, value);
                        }
                        break;

                    case "add_rfilter":
                        argument_parts = argument.split("\\s+", 2);
                        not_filter = argument_parts.length == 2 && argument_parts[0].equals("-n");
                        argument_parts = argument_parts[argument_parts.length - 1].split("->", 2);
                        int_value = Integer.parseInt(argument_parts[0]);
                        argument_parts = argument_parts[1].split("=", 2);
                        element = argument_parts[0];
                        value = argument_parts[1];
                        if(not_filter){
                            smanager.addNotReferenceFilter(int_value, element, value);
                        }
                        else {
                            smanager.addReferenceFilter(int_value, element, value);
                        }
                        break;
                            
                    case "rm_rfilter":
                        argument_parts = argument.split("\\s+", 2);
                        not_filter = argument_parts.length == 2 && argument_parts[0].equals("-n");
                        argument_parts = argument_parts[argument_parts.length - 1].split("->", 2);
                        int_value = Integer.parseInt(argument_parts[0]);
                        argument_parts = argument_parts[1].split("=", 2);
                        element = argument_parts[0];
                        value = argument_parts[1];
                        if(not_filter){
                            smanager.removeNotReferenceFilter(int_value, element, value);
                        }
                        else {
                            smanager.removeReferenceFilter(int_value, element, value);
                        }
                        break;

                    case "link":
                        argument_parts = argument.split("-", 2);
                        int_value = Integer.parseInt(argument_parts[0]);
                        String reason = argument_parts[1];
                        smanager.link(int_value, reason);
                        break;

                    case "union":
                        smanager = new StateManager(selectEnv(info, reader), filter, smanager);
                        break;

                    case "restore":
                        smanager.restore();
                        break;
                    
                    case "goback":
                        smanager.goback();
                        break;

                    default:
                        error = true;
                        System.out.println("\nInvalid command. Try again.");
                }
            }
            catch (IOException e) {
                System.out.println("Error reading input: " + e.getMessage());
            }
            catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    private static Integer selectEnv(Info info, BufferedReader reader){
        Integer selection = null;
        System.out.println("\nSelect one of the following environments to start navigating");
        info.printEnvs();
        System.out.print("Environment: ");
        try {
            selection = Integer.parseInt(reader.readLine());
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        return selection;
    }

    private static JSONObject loadJSON(){
        //String resourcePath = "/films_dataset.json";
        String resourcePath = "/test_pms.json";
        try (InputStream is = Main.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.out.println("Error: resource not found: " + resourcePath);
                return null;
            }
            String content = new String(is.readAllBytes());
            return new JSONObject(content);
        }
        catch (IOException e){
            System.out.println("Error reading dataset: " + e.getMessage());
        }
        return null;
    }
}
