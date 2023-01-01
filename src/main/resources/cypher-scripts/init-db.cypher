CREATE CONSTRAINT n10s_unique_uri FOR (r:Resource)
REQUIRE r.uri IS UNIQUE;

CALL n10s.graphconfig.init({
handleVocabUris:'KEEP',
handleMultival:'OVERWRITE',
keepLangTag:true,
handleRDFTypes:'LABELS',
keepCustomDataTypes:true
});

CALL n10s.rdf.import.fetch("https://raw.githubusercontent.com/jakubZiel/rdf-ontologies/main/vw2.owl", "Turtle")