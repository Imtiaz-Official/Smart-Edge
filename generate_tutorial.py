import math
from PIL import Image, ImageDraw

# Settings
NUM_FRAMES = 150
WIDTH, HEIGHT = 400, 300
BG_COLOR = (26, 28, 30)       # Dark surface
PANEL_COLOR = (45, 48, 51)    # Lighter panel
DOT_COLOR = (74, 158, 255)    # Blue touch indicator
EDGE_COLOR = (128, 128, 128)  # Subtle edge line

frames = []

for i in range(NUM_FRAMES):
    img = Image.new('RGB', (WIDTH, HEIGHT), BG_COLOR)
    draw = ImageDraw.Draw(img)
    
    # 1. Draw phone edge (right side)
    draw.line([(WIDTH - 2, 0), (WIDTH - 2, HEIGHT)], fill=EDGE_COLOR, width=4)
    
    # Time progression (0.0 to 1.0)
    t = i / float(NUM_FRAMES - 1)
    
    # Animation Phases:
    # 0.0 - 0.2: Finger approaches edge
    # 0.2 - 0.4: Finger swipes inward (left)
    # 0.4 - 0.7: Finger holds still (Panel slides out via spring animation)
    # 0.7 - 0.9: Finger lifts, Panel stays
    # 0.9 - 1.0: Reset
    
    # Touch dot position
    dot_x, dot_y = WIDTH + 20, HEIGHT // 2
    dot_alpha = 0
    dot_size = 30
    
    # Panel position (normally off-screen to the right)
    panel_w = 60
    panel_x = WIDTH
    
    if t < 0.2:
        # Move in
        progress = t / 0.2
        dot_x = int(WIDTH + 20 - (40 * progress))
        dot_alpha = int(255 * progress)
    elif t < 0.4:
        # Swipe left
        progress = (t - 0.2) / 0.2
        # Smooth ease out
        ease = math.sin(progress * math.pi / 2)
        dot_x = int(WIDTH - 20 - (60 * ease))
        dot_alpha = 255
    elif t < 0.7:
        # Hold (dot stays still, panel animates out)
        dot_x = WIDTH - 80
        dot_alpha = 255
        
        # Spring animation for panel
        progress = (t - 0.4) / 0.3
        # Simple spring math: 1 - e^(-st) * cos(dt)
        spring = 1.0 - math.exp(-8 * progress) * math.cos(15 * progress)
        panel_x = int(WIDTH - (panel_w * spring))
        
        # Dot pulse effect while holding
        pulse = math.sin(progress * math.pi * 4) * 5
        dot_size = int(30 + pulse)
    elif t < 0.9:
        # Lift finger
        progress = (t - 0.7) / 0.2
        dot_x = WIDTH - 80
        dot_alpha = int(255 * (1.0 - progress))
        panel_x = WIDTH - panel_w
    else:
        # Reset (panel disappears)
        dot_alpha = 0
        panel_x = WIDTH
        
    # Draw Panel
    if panel_x < WIDTH:
        # Use an overlay image to support alpha if we wanted, 
        # but solid shapes are fine for a stylized tutorial.
        draw.rounded_rectangle(
            [(panel_x, HEIGHT//2 - 80), (WIDTH + 20, HEIGHT//2 + 80)], 
            radius=24, 
            fill=PANEL_COLOR
        )
        # Draw some fake app icons inside the panel
        for j in range(3):
            icon_y = HEIGHT//2 - 50 + (j * 40)
            draw.ellipse(
                [(panel_x + 15, icon_y), (panel_x + 45, icon_y + 30)], 
                fill=(80, 85, 90)
            )

    # Draw Touch Dot
    if dot_alpha > 0:
        # Pillow doesn't do per-pixel alpha easily with ImageDraw on RGB directly,
        # so we'll simulate it by blending if necessary, but a solid dot is fine.
        # Draw outer ring
        draw.ellipse(
            [(dot_x - dot_size//2, dot_y - dot_size//2), 
             (dot_x + dot_size//2, dot_y + dot_size//2)], 
            outline=DOT_COLOR, width=2
        )
        # Draw inner core
        draw.ellipse(
            [(dot_x - 10, dot_y - 10), 
             (dot_x + 10, dot_y + 10)], 
            fill=DOT_COLOR
        )

    # Add text instructions
    font_color = (200, 200, 200)
    if t < 0.2:
        text = "Swipe from edge..."
    elif t < 0.4:
        text = "Swipe inward..."
    elif t < 0.7:
        text = "Hold down!"
    elif t < 0.9:
        text = "Panel opened"
    else:
        text = ""
        
    # Simple text drawing (default font)
    if text:
        draw.text((40, 40), text, fill=font_color)

    frames.append(img)

# Save to GIF
# The maximum framerate a standard GIF allows is 50fps (20ms). 
# Anything lower (like 16ms) triggers a legacy decoder fail-safe that clamps it to 10fps.
output_path = "c:/Users/Imtiaz/Desktop/Projetcs/Sidepanel/app/src/main/res/drawable/tutorial_anim.gif"
frames[0].save(
    output_path, 
    save_all=True, 
    append_images=frames[1:], 
    optimize=True, 
    duration=20, 
    loop=0
)

print(f"Generated {output_path}")
