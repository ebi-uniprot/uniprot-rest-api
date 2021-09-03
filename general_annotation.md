## General annotation (Comments)
Position-independent general annotations used to be found in the 'General annotation (Comments)' section in the previous version of the UniProtKB entry view. They provide any useful information about the protein, mostly biological knowledge. General annotations are frequently written in free text, although we increasingly try to standardize them and use controlled vocabulary wherever possible. The flat file and XML formats still group all general annotation together in a "Comments section" (CC, ).

The current entry view disp lays annotation by subject (Function, PTM & processing, etc), and the various "general" annotations are now distributed to the relevant new sections.

**Subsection**|**Query Field**|**Content**
:-----:|:-----:|:-----:
[Function](https://beta.uniprot.org/help/function)|cc\_function|General function(s) of a protein
[Catalytic activity](https://beta.uniprot.org/help/catalytic\_activity)|cc\_catalytic\_activity|Reaction(s) catalyzed by an enzyme
[Cofactor](https://beta.uniprot.org/help/cofactor)|cc\_cofactor|Non-protein substance required for enzyme activity
[Activity regulation](https://beta.uniprot.org/help/activity\_regulation)|cc\_activity\_regulation|Factors that regulate the activity of enzymes, but also of transporters and microbial transcription factors
[Biophysicochemical properties](https://beta.uniprot.org/help/biophysicochemical\_properties)|cc\_biophysicochemical\_properties|Biophysical and physicochemical properties
[Subunit structure](https://beta.uniprot.org/help/subunit\_structure)|cc\_subunit|Interaction with other protein(s)
[Pathway](https://beta.uniprot.org/help/pathway)|cc\_pathway|Associated metabolic pathways
[Subcellular location](https://beta.uniprot.org/help/subcellular\_location)|cc\_scl\_term|Subcellular location of the mature protein
[Tissue specificity](https://beta.uniprot.org/help/tissue\_specificity)|cc\_tissue\_specificity|Expression of the gene product in different tissues
[Developmental stage](https://beta.uniprot.org/help/developmental\_stage)|cc\_developmental\_stage|Expression of the gene product according to the cell stage and/or tissue or organism development
[Induction](https://beta.uniprot.org/help/induction)|cc\_induction|Effects of environmental factors on gene expression
[Domain](https://beta.uniprot.org/help/domain\_cc)|cc\_domain|Relevant information on protein domain(s)
[Post-translational modification](https://beta.uniprot.org/help/post-translational\_modification)|cc\_ptm|Post-translational modifications
[RNA editing](https://beta.uniprot.org/help/rna\_editing)|cc\_rna\_editing|Amino acid variation(s) due to RNA editing
[Mass spectrometry](https://beta.uniprot.org/help/mass\_spectrometry)|cc\_mass\_spectrometry|Information derived from mass spectrometry experiments
[Polymorphism](https://beta.uniprot.org/help/polymorphism)|cc\_polymorphism|Description of polymorphism(s)
[Involvement in disease](https://beta.uniprot.org/help/involvement\_in\_disease)|cc\_disease|Disease(s) associated with protein defect(s)
[Disruption phenotype](https://beta.uniprot.org/help/disruption\_phenotype)|cc\_disruption\_phenotype|Description of the effects caused by ablation of a protein-encoding gene (or of its transcript(s))
[Allergenic properties](https://beta.uniprot.org/help/allergenic\_properties)|cc\_allergen|Information relevant to allergenic proteins
[Toxic dose](https://beta.uniprot.org/help/toxic\_dose)|cc\_toxic\_dose|Lethal, paralytic, effect dose or lethal concentration of a toxin
[Biotechnological use](https://beta.uniprot.org/help/biotechnological\_use)|cc\_biotechnology|Use in a biotechnological process
[Pharmaceutical use](https://beta.uniprot.org/help/pharmaceutical\_use)|cc\_pharmaceutical|Use of as a pharmaceutical drug
[Miscellaneous](https://beta.uniprot.org/help/miscellaneous)|cc\_miscellaneous|Any relevant information that doesn’t fit in any other defined sections
[Sequence similarities](https://beta.uniprot.org/help/sequence\_similarities)|cc\_similarity|Description of the sequence similaritie(s) with other proteins and family attribution
[Caution](https://beta.uniprot.org/help/caution)|cc\_caution|Warning about possible errors and/or grounds for confusion
[Sequence caution](https://beta.uniprot.org/help/sequence\_caution)|cc\_sequence\_caution|Warning about possible errors related to the protein sequence

## Querying Comments

Individual comments can be queried using the query fields described in the tables above. Querying is of the form:

```bash
curl "http://www.ebi.ac.uk/uniprot/api/uniprotkb/search?query=FIELD:VALUE"
```

For example, to find all Human entries with catalyctic activity, we could run the following `curl` command:

```bash
curl "http://www.ebi.ac.uk/uniprot/api/uniprotkb/search?query=cc_catalytic_activity:* AND organism_id:9606"
```


### Comments evidences
For any comment field, a corresponding field exists denoting its attached evidences (for a complete list of evidences, refer to [Evidences](evidences.md). Given a comment, `cc_XXXX`, its evidences can be queried via, `ccev_XXXX`. For example, to find Human entries with comment catalytic activity and a non-traceable evidence, we can execute the following command:
```
curl "http://www.ebi.ac.uk/uniprot/api/uniprotkb/search?query=cc_catalytic_activity:* AND ccev_catalytic_activity:ECO_0000303 AND organism_id:9606"
```