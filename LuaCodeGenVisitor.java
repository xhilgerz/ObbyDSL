import java.util.LinkedHashMap;
import java.util.Map;

public class LuaCodeGenVisitor extends OBBYParserBaseVisitor<String> {

    // projectName -> stageName -> itemName -> generated code
    private final Map<String, Map<String, StringBuilder>> stageOutputs = new LinkedHashMap<>();
    private StringBuilder currentOutput = new StringBuilder();
    private String currentItemName = "";
    private String currentFolderName = "";
    private String currentProjectName = "";
    private int speed = 5;
    private int distance = 10;

    public String getProjectName() { return currentProjectName; }
    public Map<String, Map<String, StringBuilder>> getStageOutputs() { return stageOutputs; }

    private void emit(String line) {
        currentOutput.append(line).append("\n");
    }

    private void emitScript(String varName, String parentName, String source) {
        emit("local " + varName + " = Instance.new(\"Script\")");
        emit(varName + ".Parent = " + parentName);
        currentOutput.append(varName).append(".Source = [[\n");
        currentOutput.append(source);
        currentOutput.append("]]\n");
        emit("");
    }

    @Override
    public String visitProgram(OBBYParser.ProgramContext ctx) {
        currentProjectName = ctx.NAME().getText();

        for (OBBYParser.FolderContext folder : ctx.folder()) {
            visitFolder(folder);
        }

        return null;
    }

    @Override
    public String visitFolder(OBBYParser.FolderContext ctx) {
        currentFolderName = ctx.NAME().getText();
        stageOutputs.put(currentFolderName, new LinkedHashMap<>());

        for (OBBYParser.ItemContext item : ctx.item()) {
            visitItem(item);
        }

        return null;
    }

    @Override
    public String visitItem(OBBYParser.ItemContext ctx) {
        String name = ctx.NAME().getText();
        currentItemName = name;
        speed = 5;
        distance = 10;

        currentOutput = new StringBuilder();
        stageOutputs.get(currentFolderName).put(name, currentOutput);

        if (ctx.PART() != null) {
            emit("local " + name + " = Instance.new(\"Part\")");
            for (OBBYParser.PropertyContext prop : ctx.property()) {
                visitProperty(prop);
            }
            emit(name + ".Parent = workspace");
            emit("");

        } else if (ctx.K_BRICK() != null) {
            emit("local " + name + " = Instance.new(\"Part\")");
            for (OBBYParser.PropertyContext prop : ctx.property()) {
                visitProperty(prop);
            }
            emit(name + ".Parent = workspace");
            String killSource =
                "local part = script.Parent\n" +
                "part.Touched:Connect(function(hit)\n" +
                "    local humanoid = hit.Parent:FindFirstChildOfClass(\"Humanoid\")\n" +
                "    if humanoid then\n" +
                "        humanoid.Health = 0\n" +
                "    end\n" +
                "end)\n";
            emitScript(name + "Script", name, killSource);

        } else if (ctx.M_PLATFROM() != null) {
            emit("local " + name + " = Instance.new(\"Part\")");
            for (OBBYParser.PropertyContext prop : ctx.property()) {
                visitProperty(prop);
            }
            emit(name + ".Parent = workspace");
            String platformSource =
                "local TweenService = game:GetService(\"TweenService\")\n" +
                "local part = script.Parent\n" +
                "local startPos = part.Position\n" +
                "local distance = " + distance + "\n" +
                "local speed = " + speed + "\n" +
                "local tweenInfo = TweenInfo.new(distance / speed, Enum.EasingStyle.Linear, Enum.EasingDirection.Out, -1, true)\n" +
                "local tween = TweenService:Create(part, tweenInfo, {Position = startPos + Vector3.new(distance, 0, 0)})\n" +
                "tween:Play()\n";
            emitScript(name + "Script", name, platformSource);

        } else if (ctx.C_POINT() != null) {
            emit("local " + name + " = Instance.new(\"Part\")");
            for (OBBYParser.PropertyContext prop : ctx.property()) {
                visitProperty(prop);
            }
            emit(name + ".Parent = workspace");
            String checkpointSource =
                "local part = script.Parent\n" +
                "part.Touched:Connect(function(hit)\n" +
                "    local player = game.Players:GetPlayerFromCharacter(hit.Parent)\n" +
                "    if player then\n" +
                "        player.RespawnLocation = part\n" +
                "    end\n" +
                "end)\n";
            emitScript(name + "Script", name, checkpointSource);
        }

        return null;
    }

    @Override
    public String visitProperty(OBBYParser.PropertyContext ctx) {
        String n = currentItemName;

        emit(n + ".Name = \"" + n + "\"");

        if (ctx.SIZE() != null) {
            String x = ctx.INT(0).getText();
            String y = ctx.INT(1).getText();
            String z = ctx.INT(2).getText();
            emit(n + ".Size = Vector3.new(" + x + ", " + y + ", " + z + ")");

        } else if (ctx.COLOR() != null) {
            String r = visitNum(ctx.num(0));
            String g = visitNum(ctx.num(1));
            String b = visitNum(ctx.num(2));
            emit(n + ".Color = Color3.new(" + r + ", " + g + ", " + b + ")");

        } else if (ctx.MATERIAL() != null) {
            String mat = ctx.NAME().getText();
            emit(n + ".Material = Enum.Material." + mat);

        } else if (ctx.ANCHORED() != null) {
            String val = ctx.NAME().getText();
            emit(n + ".Anchored = " + val);

        } else if (ctx.SPEED() != null) {
            speed = Integer.parseInt(ctx.INT(0).getText());

        } else if (ctx.DISTANCE() != null) {
            distance = Integer.parseInt(ctx.INT(0).getText());
        }

        return null;
    }

    @Override
    public String visitNum(OBBYParser.NumContext ctx) {
        return ctx.getText();
    }
}
