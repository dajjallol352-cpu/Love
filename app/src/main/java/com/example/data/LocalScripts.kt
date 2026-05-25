package com.example.data

data class DefaultScript(
    val title: String,
    val description: String,
    val code: String,
    val category: String = "Universal"
)

object LocalScripts {
    val list = listOf(
        DefaultScript(
            title = "Infinite Yield (FE Admin)",
            description = "Script admin terpopuler untuk Roblox dengan total 250+ perintah untuk all executor.",
            code = "loadstring(game:HttpGet('https://raw.githubusercontent.com/EdgeIy/InfiniteYield/master/source'))()",
            category = "Universal"
        ),
        DefaultScript(
            title = "Simple Fly Script",
            description = "Terbangkan karakter kamu ke mana saja di game apa saja. Tekan 'E' untuk mengaktifkan/menonaktifkan.",
            code = """-- Simple Fly Script for Roblox All Executors
local player = game.Players.LocalPlayer
local character = player.Character or player.CharacterAdded:Wait()
local hrp = character:WaitForChild("HumanoidRootPart")
local humanoid = character:WaitForChild("Humanoid")

local flying = false
local speed = 50
local vn = Vector3.new(0,0,0)

local function getMoveVector()
    local cam = workspace.CurrentCamera
    local look = cam.CFrame.LookVector
    local right = cam.CFrame.RightVector
    local move = Vector3.new(0,0,0)
    
    -- basic input checks
    local UIS = game:GetService("UserInputService")
    if UIS:IsKeyDown(Enum.KeyCode.W) then move = move + look end
    if UIS:IsKeyDown(Enum.KeyCode.S) then move = move - look end
    if UIS:IsKeyDown(Enum.KeyCode.A) then move = move - right end
    if UIS:IsKeyDown(Enum.KeyCode.D) then move = move + right end
    return move.Unit
end

local function toggleFly()
    flying = not flying
    if flying then
        humanoid.PlatformStand = true
        task.spawn(function()
            local bv = Instance.new("BodyVelocity")
            bv.Velocity = Vector3.new(0,0,0)
            bv.MaxForce = Vector3.new(9e9, 9e9, 9e9)
            bv.Parent = hrp
            
            while flying do
                task.wait()
                local move = getMoveVector()
                if move.Magnitude > 0 then
                    bv.Velocity = move * speed
                else
                    bv.Velocity = Vector3.new(0,0.1,0)
                end
            end
            bv:Destroy()
            humanoid.PlatformStand = false
        end)
    end
end

-- Tekan E untuk Terbang
game:GetService("UserInputService").InputBegan:Connect(function(input, gpe)
    if gpe then return end
    if input.KeyCode == Enum.KeyCode.E then
        toggleFly()
    end
end)
print("Fly Script Loaded! Press 'E' to fly.")""",
            category = "Delta"
        ),
        DefaultScript(
            title = "Hydroxide IDE & Spier",
            description = "Sangat berguna untuk mencari remote event, memicu remote spy, dan menganalisis game.",
            code = "local owner = \"Upvxpe\"\nloadstring(game:HttpGet(\"https://raw.githubusercontent.com/Upvxpe/Hydroxide/main/source.lua\"))()",
            category = "Codex"
        ),
        DefaultScript(
            title = "Aimbot & Wallhack ESP Simple",
            description = "ESP Box basic untuk menggambar kotak di sekeliling player lawan dan memudahkan bidikan.",
            code = """-- Simple ESP Box for Roblox All Executors
local Players = game:GetService("Players")
local LocalPlayer = Players.LocalPlayer

local function createESP(player)
    if player == LocalPlayer then return end
    
    local function drawESP()
        local char = player.Character
        if char and char:FindFirstChild("HumanoidRootPart") then
            -- Check if Highlight already exists
            if not char:FindFirstChild("ESPHighlight") then
                local highlight = Instance.new("Highlight")
                highlight.Name = "ESPHighlight"
                highlight.FillColor = Color3.fromRGB(255, 0, 0)
                highlight.OutlineColor = Color3.fromRGB(255, 255, 255)
                highlight.FillTransparency = 0.5
                highlight.OutlineTransparency = 0
                highlight.Adornee = char
                highlight.Parent = char
            end
        end
    end
    
    player.CharacterAdded:Connect(function()
        task.wait(1)
        drawESP()
    end)
    
    if player.Character then
        drawESP()
    end
end

for _, p in ipairs(Players:GetPlayers()) do
    createESP(p)
end

Players.PlayerAdded:Connect(createESP)
print("Simple ESP Box loaded successfully!")""",
            category = "Universal"
        ),
        DefaultScript(
            title = "Dark Dex V3",
            description = "Explorer terbaik di Roblox. Memungkinkan Anda melihat model game, workspace, dan script script lokal game.",
            code = "loadstring(game:HttpGet(\"https://raw.githubusercontent.com/infyiff/backup/main/dex.lua\"))()",
            category = "Universal"
        ),
        DefaultScript(
            title = "Shift Lock Button Mobile",
            description = "Menambahkan tombol Shift Lock di layar untuk pengguna HP / Mobile Roblox.",
            code = "loadstring(game:HttpGet(\"https://raw.githubusercontent.com/ToraIsMe/ToraIsMe/main/0ShiftLock\"))()",
            category = "Delta"
        )
    )
}
