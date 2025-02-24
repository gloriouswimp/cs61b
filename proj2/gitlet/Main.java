package gitlet;

import static gitlet.Utils.*;
import static java.lang.System.exit;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Please enter a command.");
            exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                argsNumCheck(args, 1);
                Repository.initRepository();
                break;
            case "add":
                argsNumCheck(args, 2);
                Repository.addStage(args[1]);
                break;
            case "commit":
                if(args.length > 2) {
                    message("Incorrect operands.");
                    exit(0);
                }
                if(args.length == 1) {
                    message("Please enter a commit message.");
                    exit(0);
                }
                Repository.commit(args[1]);
                break;
            case "rm":
                argsNumCheck(args, 2);
                Repository.removeStage(args[1]);
                break;
            case "log":
                argsNumCheck(args, 1);
                Repository.log();
                break;
            case "global-log":
                argsNumCheck(args, 1);
                Repository.global_log();
                break;
            case "find":
                argsNumCheck(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                argsNumCheck(args, 1);
                Repository.status();
                break;
            case "checkout":
                if(args.length > 4 || args.length == 1) {
                    message("Please enter a commit message.");
                    exit(0);
                }
                break;
            case "branch":
                break;
            case "rm-branch":
                break;
            case "reset":
                break;
            case "merge":
                break;
            default:
                message("No command with that name exists.");
                exit(0);
        }
    }

    public static void argsNumCheck(String[] args, int validNum) {
        if(args.length != validNum) {
            message("Incorrect operands.");
            exit(0);
        }
    }
}
