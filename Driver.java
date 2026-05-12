import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Driver {

    public static void main(String[] args) throws Exception {
        CharStream input = CharStreams.fromStream(System.in);
        OBBYLexer lexer = new OBBYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OBBYParser parser = new OBBYParser(tokens);

        ParseTree tree = parser.program();
        LuaCodeGenVisitor visitor = new LuaCodeGenVisitor();
        visitor.visit(tree);

        File projectDir = new File(visitor.getProjectName());
        projectDir.mkdirs();

        for (Map.Entry<String, Map<String, StringBuilder>> stageEntry : visitor.getStageOutputs().entrySet()) {
            File stageDir = new File(projectDir, stageEntry.getKey());
            stageDir.mkdirs();

            for (Map.Entry<String, StringBuilder> itemEntry : stageEntry.getValue().entrySet()) {
                File luaFile = new File(stageDir, itemEntry.getKey() + ".lua");
                try (PrintWriter writer = new PrintWriter(new FileWriter(luaFile))) {
                    writer.print(itemEntry.getValue().toString());
                }
                System.out.println("Generated: " + luaFile.getPath());
            }
        }
    }
}
