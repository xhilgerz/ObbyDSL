ObbDSL - Obby Domain-Specific Language
=======================================

A DSL for defining Roblox obstacle course (obby) levels using a concise,
readable syntax. The compiler parses a .obby source file and generates a
directory of Roblox Lua scripts organized by stage and item.


ASSUMED DEPENDENCIES (already installed on grader's system)
------------------------------------------------------------
- Java (JDK 11 or higher)
- ANTLR 4 runtime JAR (antlr-4.13.2-complete.jar or compatible)


INSTALLING DEPENDENCIES
-----------------------
1. Install Java if not present:
       brew install openjdk
   Or download from https://adoptium.net

2. Download the ANTLR 4 complete JAR:
       curl -O https://www.antlr.org/download/antlr-4.13.2-complete.jar

   Place the JAR somewhere accessible (e.g., /usr/local/lib/) and set
   an environment variable for convenience:
       export ANTLR_JAR=/path/to/antlr-4.13.2-complete.jar


BUILD AND RUN
-------------
All commands should be run from the project root directory.

Step 1 - Generate the lexer/parser from the grammar files:
    java -jar $ANTLR_JAR -visitor OBBYLexer.g4 OBBYParser.g4

Step 2 - Compile all Java source files:
    javac -cp .:$ANTLR_JAR *.java

Step 3 - Run the compiler with a .obby input file:
    java -cp .:$ANTLR_JAR Driver < test.obby

The compiler reads from stdin and writes the generated Lua files to disk.
A directory named after the project is created in the current working directory.


OUTPUT STRUCTURE
----------------
For each project the compiler creates one directory per folder (stage), with
one .lua file per item inside it:

    Test/
      Stage1/
        StartPlatform.lua
        CP1.lua
      Stage2/
        LavaTile.lua
        Swinger.lua
        CP2.lua

Each .lua file contains the Roblox Lua code for that item. Special item types
(kill_brick, moving_platform, checkpoint) also include an embedded Script
instance with the appropriate game logic.


DSL SYNTAX
----------
    project "ProjectName" {
        folder "StageName" {
            part "Name" {
                size:     <x> <y> <z>
                color:    <r> <g> <b>
                material: <MaterialName>
                anchored: <true|false>
            }
            kill_brick "Name" {
                size:  <x> <y> <z>
                color: <r> <g> <b>
            }
            moving_platform "Name" {
                size:     <x> <y> <z>
                speed:    <int>
                distance: <int>
            }
            checkpoint "Name" {
                size: <x> <y> <z>
            }
        }
    }

Item types:
  part             - a standard static Roblox part
  kill_brick       - kills any player that touches it
  moving_platform  - oscillates using TweenService; requires speed: and distance:
  checkpoint       - sets the player's respawn location on touch

Properties:
  size:     x y z     - Vector3 dimensions (integers)
  color:    r g b     - Color3 values (0.0-1.0)
  material: Name      - Enum.Material value (e.g. SmoothPlastic, Neon)
  anchored: true|false
  speed:    int       - travel speed in studs/sec (moving_platform only)
  distance: int       - studs to travel (moving_platform only)


EXAMPLE INPUT (test.obby)
--------------------------
    project "Test" {
      folder "Stage1" {
        part "StartPlatform" {
          size: 20 1 20
          color: 0.4 0.8 0.4
          material: SmoothPlastic
          anchored: true
        }
        checkpoint "CP1" { size: 4 6 4 }
      }
      folder "Stage2" {
        kill_brick      "LavaTile" { size: 4 1 4  color: 1 0.2 0 }
        moving_platform "Swinger"  { size: 8 1 4  speed: 3  distance: 10 }
        checkpoint      "CP2"      { size: 4 6 4 }
      }
    }


EXAMPLE OUTPUT (Test/Stage1/StartPlatform.lua)
-----------------------------------------------
    local StartPlatform = Instance.new("Part")
    StartPlatform.Size = Vector3.new(20, 1, 20)
    StartPlatform.Color = Color3.new(0.4, 0.8, 0.4)
    StartPlatform.Material = Enum.Material.SmoothPlastic
    StartPlatform.Anchored = true
    StartPlatform.Parent = workspace

EXAMPLE OUTPUT (Test/Stage2/Swinger.lua)
-----------------------------------------
    local Swinger = Instance.new("Part")
    Swinger.Size = Vector3.new(8, 1, 4)
    Swinger.Parent = workspace
    local SwingerScript = Instance.new("Script")
    SwingerScript.Parent = Swinger
    SwingerScript.Source = [[
    local TweenService = game:GetService("TweenService")
    local part = script.Parent
    local startPos = part.Position
    local distance = 10
    local speed = 3
    local tweenInfo = TweenInfo.new(distance / speed,
        Enum.EasingStyle.Linear, Enum.EasingDirection.Out, -1, true)
    local tween = TweenService:Create(part, tweenInfo,
        {Position = startPos + Vector3.new(distance, 0, 0)})
    tween:Play()
    ]]
