CREATE CONSTRAINT n10s_unique_uri ON (r:Resource)
ASSERT r.uri IS UNIQUE;

CALL n10s.graphconfig.init({
handleVocabUris: 'KEEP',
handleMultival: 'OVERWRITE',
keepLangTag: true,
handleRDFTypes: 'LABELS',
keepCustomDataTypes: true
});