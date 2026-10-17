# App-specific R8 rules. The optimized Android defaults and library consumer
# rules are loaded by AGP. Manifest components and XML views are handled by AGP.
# The app uses explicit JSON keys and no reflection in production code;
# no package-wide keep/dontwarn rules are required.
# Keep shrinking, optimization and obfuscation enabled. Add only targeted rules
# if a new dependency or a reproducible release-only failure requires them.
