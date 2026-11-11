# Artwork provenance

Generated with the built-in image-generation tool for this project on 2026-10-29.
The game does not contain art extracted from the Instagram reel.

## Background

Saved in `app/src/main/res/drawable-nodpi/nebula.png`.

Prompt: "Use case: stylized-concept. Asset type: production background texture for a premium portrait mobile space shooter. Create a tall portrait 2:3 deep-space environment, almost black navy with subtle indigo and cyan gaseous nebula wisps near the edges, small distant stars, a beautifully rendered crescent planet partly cropped in the upper right with thin blue atmosphere and shaded surface. Cinematic physically textured astronomical art, restrained luminosity so bright ships and bullets will remain readable on top. Large dark quiet central corridor and dark lower half for gameplay, no spacecraft, no interface, no lettering, no logos, no big bright stars, no borders. Rich dimensional detail and elegant color grading. This is a background-only game asset, not a screenshot."

## Ship atlas

Saved in `app/src/main/res/drawable-nodpi/ships.png` with transparency. Sprite source
rectangles are defined in `GameArt`. The renderer adds exhaust, glow and impact effects.

Version 0.3.0 also includes Falcon and Bastion player hulls drawn by original vector
code in `GameArt.buildChassis`. Their metallic panels, cockpit lights, armor and
wing silhouettes are rendered once into 512×512 transparent bitmaps at startup.
These two hulls do not use additional generated or third-party image assets.

Prompt: "Use case: stylized-concept. Asset type: a production 2 by 2 sprite atlas for a premium top-down 2D space shooter, square image with transparent background. Four separate beautifully rendered hard-surface spacecraft, one centered precisely in each equal quadrant. Top left: sleek heroic small interceptor pointing straight UP, silver titanium armor, angular swept wings, dark blue cockpit, subtle cyan accent strips. Top right: enemy angular drone pointing straight DOWN, dark gunmetal armor, crimson glowing reactor and wing accents. Bottom left: heavier gunship pointing straight DOWN, broad armored wings, bronze orange armor, orange reactor. Bottom right: massive intimidating boss dreadnought pointing straight DOWN, broad symmetrical silhouette, layered dark metallic plates, bright magenta power core, twin heavy cannon arms. ALL strict orthographic top view, no perspective, realistic illustrated 3D material shading, extremely crisp silhouettes. Each ship fits entirely inside its quadrant with at least 15 percent transparent padding, even spacing, no touching or crossing quadrant boundaries. No engine exhaust trails, no shadows outside ships, no background, no grid lines, no labels, no lettering. Entire background is transparent alpha."
