### UniProtKB column names for programmatic access

This document lists the differences between the returned columns by RESTful APIs.
User can ask required columns returned by an API by passing the _Returned Field_ in the request url 
and the response will have those requested fields.

For example, to get UniProtKB entry with accession and proteomes in new API run the below `curl` command:
```bash
curl https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?query=human&fields=accession,xref_proteomes
```
Equivalent request in the current website would be :
```
https://www.uniprot.org/uniprot/?query=human&columns=id,proteome  
```
The `Label` is the readable name of the returned field. The `Label`  is shown on the website/TSV or in Excel format.

### Names & Taxonomy

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Entry|id|accession
Entry name|entry name|id
Gene names|genes|gene\_names
Gene names (primary)|genes(PREFERRED)|gene\_primary
Gene names (synonym)|genes(ALTERNATIVE)|gene\_synonym
Gene names (ordered locus)|genes(OLN)|gene\_oln
Gene names (ORF)|genes(ORF)|gene\_orf
Organism|organism|organism\_name
Organism ID|organism-id|organism\_id
Protein names|protein names|protein\_name
Proteomes|proteome|xref\_proteomes
Taxonomic lineage|lineage(ALL)|lineage
Virus hosts|virus hosts|virus\_hosts

_* Label in old and new API unless otherwise specified_

### Sequences

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Alternative products|comment(ALTERNATIVE PRODUCTS)|cc\_alternative\_products
Alternative sequence|feature(ALTERNATIVE SEQUENCE)|ft\_var\_seq
Erroneous gene model prediction|comment(ERRONEOUS GENE MODEL PREDICTION)|error\_gmodel\_pred
Fragment|fragment|fragment
Gene encoded by|encodedon|organelle
Length|length|length
Mass|mass|mass
Mass spectrometry|comment(MASS SPECTROMETRY)|cc\_mass\_spectrometry
Natural variant|feature(NATURAL VARIANT)|ft\_variant
Non-adjacent residues|feature(NON ADJACENT RESIDUES)|ft\_non\_cons
Non-standard residue|feature(NON STANDARD RESIDUE)|ft\_non\_std
Non-terminal residue|feature(NON TERMINAL RESIDUE)|ft\_non\_ter
Polymorphism|comment(POLYMORPHISM)|cc\_polymorphism
RNA editing|comment(RNA EDITING)|cc\_rna\_editing
Sequence|sequence|sequence
Sequence caution|comment(SEQUENCE CAUTION)|cc\_sequence\_caution
Sequence conflict|feature(SEQUENCE CONFLICT)|ft\_conflict
Sequence uncertainty|feature(SEQUENCE UNCERTAINTY)|ft\_unsure
Sequence version|version(sequence)|sequence\_version

### Function

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Absorption|comment(ABSORPTION)|absorption
Active site|feature(ACTIVE SITE)|ft\_act\_site
Activity regulation|comment(ACTIVITY REGULATION)|cc\_activity\_regulation
Binding site|feature(BINDING SITE)|ft\_binding
Calcium binding|chebi|ft\_ca\_bind
Catalytic activity|chebi(Catalytic activity)|cc\_catalytic\_activity
Cofactor|chebi(Cofactor)|cc\_cofactor
DNA binding|feature(DNA BINDING)|ft\_dna\_bind
EC number|ec|ec
Function [CC]i|comment(FUNCTION)|cc\_function
Kinetics|comment(KINETICS)|kinetics
Metal binding|feature(METAL BINDING)|ft\_metal
Nucleotide binding|feature(NP BIND)|ft\_np\_bind
Pathway|comment(PATHWAY)|cc\_pathway
pH dependence|comment(PH DEPENDENCE)|ph\_dependence
Redox potential|comment(REDOX POTENTIAL)|redox\_potential
Rhea ID|rhea-id|NA
Site|feature(SITE)|ft\_site
Temperature dependence|comment(TEMPERATURE DEPENDENCE)|temp\_dependence

### Miscellaneous

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Annotation|annotation score|annotation\_score
Caution|comment(CAUTION)|cc\_caution
Comment Count* |NA|comment\_count
Features|features|feature
Feature Count*|NA|feature \_count
Keyword ID|keyword-id|keywordid
Keywords|keywords|keyword
Matched text|context|NA
Miscellaneous [CC]i|comment(MISCELLANEOUS)|cc\_miscellaneous
Protein existence|existence|protein\_existence
Reviewed|reviewed|reviewed
Tools|tools|tools
UniParc|uniparcid|uniparc\_id

### Interaction

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Interacts with|interactor|cc\_interaction
Subunit structure[CC]|comment(SUBUNIT)|cc\_subunit

### Expression

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Developmental stage|comment(DEVELOPMENTAL STAGE)|cc\_developmental\_stage
Induction|comment(INDUCTION)|cc\_induction
Tissue specificity|comment(TISSUE SPECIFICITY)|cc\_tissue\_specificity

### Gene Ontology (GO)

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Gene ontology (biological process)|go(biological process)|go\_p
Gene ontology (cellular component)|go(cellular component)|go\_c
Gene ontology (GO)|go|go
Gene ontology (molecular function)|go(molecular function)|go\_f
Gene ontology IDs|go-id|go\_id

### Pathology & Biotech

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Allergenic properties|comment(ALLERGEN)|cc\_allergen
Biotechnological use|comment(BIOTECHNOLOGY)|cc\_biotechnology
Disruption phenotype|comment(DISRUPTION PHENOTYPE)|cc\_disruption\_phenotype
Involvement in disease|comment(DISEASE)|cc\_disease
Mutagenesis|feature(MUTAGENESIS)|ft\_mutagen
Pharmaceutical use|comment(PHARMACEUTICAL)|cc\_pharmaceutical
Toxic dose|comment(TOXIC DOSE)|cc\_toxic\_dose

### Subcellular location

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Intramembrane|feature(INTRAMEMBRANE)|ft\_intramem
Subcellular location[CC]|comment(SUBCELLULAR LOCATION)|cc\_subcellular\_location
Topological domain|feature(TOPOLOGICAL DOMAIN)|ft\_topo\_dom
Transmembrane|feature(TRANSMEMBRANE)|ft\_transmem

### PTM / Processsing

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Chain|feature(CHAIN)|ft\_chain
Cross-link|feature(CROSS LINK)|ft\_crosslnk
Disulfide bond|feature(DISULFIDE BOND)|ft\_disulfid
Glycosylation|feature(GLYCOSYLATION)|ft\_carbohyd
Initiator methionine|feature(INITIATOR METHIONINE)|ft\_init\_met
Lipidation|feature(LIPIDATION)|ft\_lipid
Modified residue|feature(MODIFIED RESIDUE)|ft\_mod\_res
Peptide|feature(PEPTIDE)|ft\_peptide
Post-translational modification|comment(PTM)|cc\_ptm
Propeptide|feature(PROPEPTIDE)|ft\_propep
Signal peptide|feature(SIGNAL)|ft\_signal
Transit peptide|feature(TRANSIT)|ft\_transit

### Structure

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
3D|3d|structure\_3d
Beta strand|feature(BETA STRAND)|ft\_strand
Helix|feature(HELIX)|ft\_helix
Turn|feature(TURN)|ft\_turn

### Publications

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Mapped PubMed ID|citationmapping|NA
PubMed ID|citation|lit\_pubmed\_id

### Date of

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Date of creation|created|date\_created
Date of last modification|last-modified|date\_modified
Date of last sequence modification|sequence-modified|date\_sequence\_modified
Entry version|version(entry)|version

### Family & Domains

**Label***|**Returned Field**|**New Returned Field**
:-----:|:-----:|:-----:
Coiled coil|feature(COILED COIL)|ft\_coiled
Compositional bias|feature(COMPOSITIONAL BIAS)|ft\_compbias
Domain[CC]|comment(DOMAIN)|cc\_domain
Domain[FT]|feature(DOMAIN EXTENT)|ft\_domain
Motif|feature(MOTIF)|ft\_motif
Protein families|families|protein\_families
Region|feature(REGION)|ft\_region
Repeat|feature(REPEAT)|ft\_repeat
Sequence similarities|comment(SIMILARITY)|NA
Zinc finger|feature(ZINC FINGER)|ft\_zn\_fing

