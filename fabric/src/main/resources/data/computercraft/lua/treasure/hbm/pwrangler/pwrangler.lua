local pwr = peripheral.find("ntm_pwr_control")
if not pwr then error("No ntm_pwr_control attached", 0) end
local monitor = peripheral.find("monitor", function(_, m) return m.isColour() end)
if not monitor then error("No advanced monitor attached", 0) end
local monitorName = peripheral.getName(monitor)

-- The layout spans columns 60..112 and rows 4..48 of a 160-column OpenComputers screen; centre that box.
local LAYOUT_W, LAYOUT_H = 53, 45
monitor.setTextScale(0.5)
local screenW, screenH = monitor.getSize()
if screenW < LAYOUT_W or screenH < LAYOUT_H then
    error("PWRangler needs an advanced monitor of at least " .. LAYOUT_W .. "x" .. LAYOUT_H ..
            " characters (3x4 blocks)", 0)
end
local ox = math.floor((screenW - LAYOUT_W) / 2) - 59
local oy = math.floor((screenH - LAYOUT_H) / 2) - 3

-- OpenComputers gpu over the monitor: 24-bit colours take palette slots, the last one repainted on demand.
-- slot 15 stays black so monitor.clear() keeps clearing to black
local slots, nextSlot, spare = {[0x000000] = 15}, 0, 14
monitor.setPaletteColour(colours.black, 0x000000)
local function slot(rgb)
    local s = slots[rgb]
    if s == nil then
        if nextSlot < spare then
            s = nextSlot
            nextSlot = nextSlot + 1
            slots[rgb] = s
            monitor.setPaletteColour(2 ^ s, rgb)
        else
            monitor.setPaletteColour(2 ^ spare, rgb)
            return colours.toBlit(2 ^ spare)
        end
    end
    return colours.toBlit(2 ^ s)
end
-- fixed colours first, so only heat gradient steps can land on the repainted slot
for _, rgb in ipairs({0xFFFFFF, 0xAAAAAA, 0xFF0000, 0xAA0000, 0x00FF00, 0x00AA00, 0xFF0099, 0x00FFFF}) do
    slot(rgb)
end

local foreground, background = 0xFFFFFF, 0x000000
local gpu = {}
function gpu.setForeground(rgb) foreground = rgb end
function gpu.setBackground(rgb) background = rgb end
function gpu.set(x, y, text)
    local fg, bg = slot(foreground), slot(background)
    local chars, fgs, bgs = {}, {}, {}
    for _, code in utf8.codes(text) do
        local c, f, b = nil, fg, bg
        if code == 0x2588 then c, b = " ", fg          -- full block
        elseif code == 0x2580 then c = "\143"          -- upper half block
        elseif code == 0x2584 then c, f, b = "\131", bg, fg -- lower half block
        elseif code == 0x2503 then c = "|"
        elseif code == 0x2192 then c = "\26"
        elseif code == 0x2190 then c = "\27"
        elseif code == 0x0394 then c = "\30"
        else c = utf8.char(code) end
        chars[#chars + 1], fgs[#fgs + 1], bgs[#bgs + 1] = c, f, b
    end
    monitor.setCursorPos(x + ox, y + oy)
    monitor.blit(table.concat(chars), table.concat(fgs), table.concat(bgs))
end
function gpu.fill(x, y, w, h, char)
    if w < 1 then return end
    local row = string.rep(char, w)
    for i = 0, h - 1 do gpu.set(x, y + i, row) end
end

colorGradient = {0x00FF00, 0x6BEE00, 0x95DB00, 0xB0C800, 0xC5B400, 0xD79F00, 0xE68700, 0xF46900, 0xFC4700, 0xFF0000}
coreHeatESTOP = true
coolantLossESTOP = true
hotCoolantESTOP = true

local const = {}
local initialized = {}
local mt = {
    __newindex = function(t, k, v)
        if not initialized[k] then
            rawset(t, k, v)
            initialized[k] = true
        else
            error(k .. " is a constant")
        end
    end
}
setmetatable(const, mt)

_, _, const.coreHeatCapacity = pwr.getHeat()
const.fullCoreHeatMAX = const.coreHeatCapacity * 0.9
const.coldCoolantLevelMIN = 10000
const.hotCoolantLevelMAX = 0.5

runSig = true

coldCoolantLevel = 0
coldCoolantOutflow = 0
prevCoolantFlow = 0

hotCoolantLevel = 0
hotCoolantOutflow = 0
prevHotCoolantFlow = 0

monitor.setBackgroundColour(colours.black)
monitor.clear()

-- Button Bullshit
function newButton(x, y, width, height, colorUp, colorDown, func)
    local button = {xpos = 0, ypos = 0, width = 0, height = 0, colorUp = 0, colorDown = 0, func = nil}
    button.xpos = x
    button.ypos = y
    button.width = width
    button.height = height
    button.colorUp = colorUp
    button.colorDown = colorDown
    button.func = func
    return button
end

function drawButton(button, color)
    gpu.setBackground(color)
    gpu.fill(button.xpos, button.ypos, button.width, button.height, " ")
    gpu.setBackground(0x000000)
end

-- a monitor reports a touch as press and release at once
function buttonTouch(x, y)
    for _, b in pairs(buttons) do
        if((x>=b.xpos) and (x<(b.xpos+b.width)) and (y>=b.ypos) and (y<(b.ypos+b.height)) ) then
            drawButton(b, b.colorDown)
            b.func()
        end
    end
end
--Button bullshit ends

buttons = {}

local deltas = {1,5,10} -- This is very bad. Need new buttons
for i, d in ipairs(deltas) do
    buttons[i] = newButton(61+(i-1)*7, 6, 6, 2, 0xFFFFFF, 0xAAAAAA, function() pwr.setLevel(pwr.getLevel()+d) end)
    buttons[i+3] = newButton(61+(i-1)*7, 9, 6, 2, 0xFFFFFF, 0xAAAAAA, function() pwr.setLevel(pwr.getLevel()-d) end)
end

buttons[7] = newButton(82, 6, 11, 5, 0xFF0000, 0xAA0000, function() pwr.setLevel(100) end)
buttons[8] = newButton(94, 6, 12, 2, 0x00FF00, 0x00AA00, function() coreHeatESTOP = not coreHeatESTOP if coreHeatESTOP == true then buttons[8].colorUp = 0x00FF00 buttons[8].colorDown = 0x00AA00 else buttons[8].colorUp = 0xFF0000 buttons[8].colorDown = 0xAA0000 end end)
buttons[9] = newButton(94, 9, 12, 2, 0x00FF00, 0x00AA00, function() coolantLossESTOP = not coolantLossESTOP if coolantLossESTOP == true then buttons[9].colorUp = 0x00FF00 buttons[9].colorDown = 0x00AA00 else buttons[9].colorUp = 0xFF0000 buttons[9].colorDown = 0xAA0000 end  end)

buttons[10] = newButton(107, 8, 5, 3, 0xFF0000, 0xAA0000, function() runSig = false end)

gpu.setForeground(0xAAAAAA)

--Control rods
gpu.fill(60,4,54,8,"█")

--Outlet
gpu.fill(91,13,16,8,"█")

--Inlet
gpu.fill(91,30,16,8,"█")

gpu.set(61,13,"    █████████████████████")
gpu.set(61,14,"     █ █ █ █ █ █ █ █ █ █")
gpu.set(61,15,"     █ █ █▄█▄█▄█▄█▄█ █ █")
gpu.set(61,16,"    ▄█████▀█▀█▀█▀█▀█████▄")
gpu.set(61,17,"  ▄███▀█ █ █ █ █ █ █ █▀███▄")
gpu.set(61,18," ▄██ █ █ █ █ █ █ █ █ █ █ ██▄")
gpu.set(61,19," ██                       ██")
gpu.set(61,20,"██▀ █████████████████████ ▀██")
gpu.set(61,21,"██  █████████████████████  ██▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄")
gpu.set(61,22,"██  █                   █  ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀")
gpu.set(61,23,"██  █████████████████████  → → → → → → → → → →")
gpu.set(61,24,"██  █                   █  ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄")
gpu.set(61,25,"██  █████████████████████  ██▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀")
gpu.set(61,26,"██  █                   █  ██")
gpu.set(61,27,"██  █████████████████████  ██")
gpu.set(61,28,"██  █                   █  ██")
gpu.set(61,29,"██  █████████████████████  ██")
gpu.set(61,30,"██  █                   █  ██")
gpu.set(61,31,"██  █████████████████████  ██")
gpu.set(61,32,"██                         ██")
gpu.set(61,33,"██                         ██")
gpu.set(61,34,"██                         ██")
gpu.set(61,35,"██                         ██")
gpu.set(61,36,"██                         ██")
gpu.set(61,37,"██                         ██")
gpu.set(61,38,"██                         ██▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄")
gpu.set(61,39,"██                         ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀")
gpu.set(61,40,"██                         ← ← ← ← ← ← ← ← ← ←")
gpu.set(61,41,"██                         ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄")
gpu.set(61,42,"██                         ██▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀")
gpu.set(61,43,"██▄                       ▄██")
gpu.set(61,44," ██                       ██")
gpu.set(61,45," ▀██                     ██▀")
gpu.set(61,46,"  ▀██▄▄               ▄▄██▀")
gpu.set(61,47,"    ▀▀███▄▄▄▄▄▄▄▄▄▄▄███▀▀")
gpu.set(61,48,"        ▀▀▀▀▀▀▀▀▀▀▀▀")

gpu.setBackground(0xAAAAAA)
gpu.setForeground(0x000000)

gpu.set(70,4,"CONTROL RODS")
gpu.set(61,5,"INS+1  INS+5  INS+10")
gpu.set(61,8,"RET+1  RET+5  RET+10")

gpu.set(85,5,"ESTOP")
gpu.set(107,5,"LEVEL")
gpu.set(107,7,"QUIT")

gpu.set(94,5,"OVHEAT ESTOP")
gpu.set(94,8,"NOCOOL ESTOP")

gpu.set(95,13,"OUTFLOW")
gpu.set(92,14,"BUFFER")
gpu.set(99,14,"HOTΔ")

gpu.set(95,30,"INFLOW")
gpu.set(92,31,"BUFFER")
gpu.set(99,31,"COOLΔ")

gpu.set(69,20,"REACTOR  CORE")
gpu.set(71,21,"CORE HEAT:")
gpu.set(71,23,"HULL HEAT:")
gpu.set(71,25,"CORE FLUX:")
gpu.set(68,27,"COLD HEATEX LVL:")
gpu.set(69,29,"HOT HEATEX LVL:")
gpu.setBackground(0x000000)

gpu.setForeground(0xFFFFFF)
gpu.fill(107,6,5,1,"█")

--Outflow Buffer
gpu.fill(92,15,6,5,"█")

--CoolDelta
gpu.fill(99,15,7,1,"█")

--HotDelta

gpu.set(66,19,"┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃")
for y=22,30,2 do
    gpu.fill(66,y,19,1,"█")
end
gpu.set(66,32,"┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃ ┃")
gpu.setForeground(0xAAAAAA)

gpu.setForeground(0x000000)
gpu.setBackground(0xFFFFFF)
gpu.set(83,22,"TU")
gpu.set(83,24,"TU")
gpu.setForeground(0xFFFFFF)
gpu.setBackground(0x000000)

local function touches()
    while true do
        local _, side, x, y = os.pullEvent("monitor_touch")
        if side == monitorName then buttonTouch(x - ox, y - oy) end
    end
end

local function control()
    while (runSig == true) do
        rodLevel = pwr.getLevel()

        coreHeat = pwr.getHeat()
        coreHeat = math.floor(coreHeat / (const.coreHeatCapacity / 10))

        for _, b in pairs(buttons) do
            drawButton(b, b.colorUp)
        end

        for j=math.floor(rodLevel/10),10 do
            gpu.fill(64+(j*2), 33, 1, 10, " ")
        end

        for j=1,math.floor(rodLevel/10) do
            gpu.fill(64+(j*2), 33, 1, 10, "┃")
        end

        gpu.fill(64+(math.ceil(rodLevel/10)*2), 33, 1, math.floor(math.fmod(rodLevel,10)), "┃")

        for j=0,20,2 do
            gpu.setForeground(colorGradient[coreHeat+1])
            gpu.fill(65+j, 33, 1, 9, "█")
            gpu.setForeground(0xAAAAAA)
        end

        gpu.setBackground(0xFFFFFF)

        gpu.setForeground(0xFFFFFF)
        for y = 22, 30, 2 do
            gpu.fill(66,y,19,1,"█")
        end

        gpu.fill(92,15,6,5,"█")
        gpu.fill(92,32,6,5,"█")

        gpu.fill(99,15,7,1,"█")
        gpu.fill(99,32,7,1,"█")

        prevCoolantFlow = coldCoolantLevel
        prevHotCoolantFlow = hotCoolantLevel

        fullCoreHeat, fullHullHeat = pwr.getHeat()
        coldCoolantLevel, _, hotCoolantLevel, maxHotCoolantLevel = pwr.getCoolantInfo()

        coldCoolantOutflow = coldCoolantLevel - prevCoolantFlow
        hotCoolantOutflow = hotCoolantLevel - prevHotCoolantFlow

        gpu.setForeground(0xFF0099)
        gpu.fill(92,15+(5-math.floor(hotCoolantLevel/25600)),6,math.floor(hotCoolantLevel/25600), "█")
        gpu.setForeground(0x000000)

        gpu.setForeground(0x00FFFF)
        gpu.fill(92,32+(5-math.floor(coldCoolantLevel/25600)),6,math.floor(coldCoolantLevel/25600), "█")
        gpu.setForeground(0x000000)

        gpu.set(66,22,tostring(fullCoreHeat)) -- What the heck? This is too declarative!
        gpu.set(66,24,tostring(fullHullHeat)) -- Will fix that garbage later :P
        gpu.set(66,26,tostring(pwr.getFlux()))
        gpu.set(66,28,tostring(coldCoolantLevel))
        gpu.set(66,30,tostring(hotCoolantLevel))

        gpu.set(99,15,tostring(hotCoolantOutflow))
        gpu.set(99,32,tostring(coldCoolantOutflow))

        gpu.set(107,6,"   ")
        gpu.set(107,6,tostring(pwr.getLevel()))

        gpu.setBackground(0x000000)
        gpu.setForeground(0xFFFFFF)

        if (coreHeatESTOP == true) and (fullCoreHeat) > const.fullCoreHeatMAX then
            pwr.setLevel(100)
        end

        if (coolantLossESTOP == true) and (coldCoolantLevel) < const.coldCoolantLevelMIN then
            pwr.setLevel(100)
        end

        if (hotCoolantESTOP == true) and (hotCoolantLevel) > const.hotCoolantLevelMAX * maxHotCoolantLevel then
            pwr.setLevel(100)
        end

        sleep(0.25)
    end
end

parallel.waitForAny(control, touches)

monitor.setBackgroundColour(colours.black)
monitor.clear()
for i = 0, 15 do
    monitor.setPaletteColour(2 ^ i, term.nativePaletteColour(2 ^ i))
end
