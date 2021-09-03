
---
title: Sequence annotation (Features)
categories: manual,Technical
---

Sequence annotations describe regions or sites of interest in the protein sequence, such as post-translational modifications, binding sites, enzyme active sites, local secondary structure or other characteristics reported in the cited references. Sequence conflicts between references are also described in this manner.

Sequence annotations (position-specific annotations) used to be found in the 'Sequence annotation (Features)' section in the previous version of the UniProtKB entry view. The flat file and XML formats still group all position-specific annotation together in a "feature table" (FT, <feature>). Each sequence annotation consists of a "feature key", "from" and "to" positions as well as a short description.

The current entry view displays annotation by subject (Function, PTM & processing, etc), and the various position-specific annotations are now distributed to the relevant new sections.
                   
## Feature types

### Molecule processing

| **Subsection** | **Query Field** | **Content** |
|----------------|-----------------|-------------|
| [Initiator methionine](http://www.uniprot.org/manual/init_met) | ft_init_met | Cleavage of the initiator methionine |
| [Signal](http://www.uniprot.org/manual/signal) | ft_signal | Sequence targeting proteins to the secretory pathway or periplasmic space |
| [Transit peptide](http://www.uniprot.org/manual/transit) | ft_transit | Extent of a transit peptide for organelle targeting             |
| [Propeptide](http://www.uniprot.org/manual/propep) | ft_propep | Part of a protein that is cleaved during maturation or activation      |
| [Chain](http://www.uniprot.org/manual/chain) | ft_chain | Extent of a polypeptide chain in the mature protein                          |
| [Peptide](http://www.uniprot.org/manual/peptide) | ft_peptide | Extent of an active peptide in the mature protein                        |

### Regions

| **Subsection** | **Query Field** | **Content** |
|----------------|-----------------|-------------|
| [Topological domain](http://www.uniprot.org/manual/topo_dom) | ft_topo_dom | Location of non-membrane regions of membrane-spanning proteins|
| [Transmembrane](http://www.uniprot.org/manual/transmem) | ft_transmem | Extent of a membrane-spanning region                               |
| [Intramembrane](http://www.uniprot.org/manual/intramem) | ft_intramem | Extent of a region located in a membrane without crossing it       |
| [Domain](http://www.uniprot.org/manual/domain) | ft_domain | Position and type of each modular protein domain                            |
| [Repeat](http://www.uniprot.org/manual/repeat) | ft_repeat | Positions of repeated sequence motifs or repeated domains                   |
| [Calcium binding](http://www.uniprot.org/manual/ca_bind) | ft_ca_bind | Position(s) of calcium binding region(s) within the protein       |
| [Zinc finger](http://www.uniprot.org/manual/zn_fing) | ft_zn_fing | Position(s) and type(s) of zinc fingers within the protein           |
| [DNA binding](http://www.uniprot.org/manual/dna_bind)  | ft_dna_bind | Position and type of a DNA\-binding domain                          |
| [Nucleotide binding](http://www.uniprot.org/manual/np_bind)  | ft_np_bind | Nucleotide phosphate binding region                           |
| [Region](http://www.uniprot.org/manual/region) | ft_region | Region of interest in the sequence                                         |
| [Coiled coil](http://www.uniprot.org/manual/coiled) | ft_coiled | Positions of regions of coiled coil within the protein                |
| [Motif](http://www.uniprot.org/manual/motif) | ft_motif | Short (up to 20 amino acids) sequence motif of biological interest           |
| [Compositional bias](http://www.uniprot.org/manual/compbias) | ft_compbias | Region of compositional bias in the protein                  |

### Sites

| **Subsection** | **Query Field** | **Content** |
|----------------|-----------------|-------------|
| [Active site](http://www.uniprot.org/manual/act_site) | ft_act_site | Amino acid(s) directly involved in the activity of an enzyme
| [Metal binding](http://www.uniprot.org/manual/metal) | ft_metal | Binding site for a metal ion
| [Binding site](http://www.uniprot.org/manual/binding) | ft_binding | Binding site for any chemical group (co-enzyme, prosthetic group, etc.)
| [Site](http://www.uniprot.org/manual/site) | ft_site | Any interesting single amino acid site on the sequence

### Amino acid modifications

| **Subsection** | **Query Field** | **Content** |
|----------------|-----------------|-------------|
| [Non-standard residue](http://www.uniprot.org/manual/non_std) | ft_non_std | Occurence of non-standard amino acids (selenocysteine and pyrrolysine) in the protein sequence  | 
| [Modified residue](http://www.uniprot.org/manual/mod_res) | ft_mod_res | Modified residues excluding lipids, glycans and protein cross-links                                 | 
| [Lipidation](http://www.uniprot.org/manual/lipid) | ft_lipid | Covalently attached lipid group(s)                                                                          | 
| [Glycosylation](http://www.uniprot.org/manual/carbohyd) | ft_carbohyd | Covalently attached glycan group(s)                                                                   | 
| [Disulfide bond](http://www.uniprot.org/manual/disulfid) | ft_disulfid | Cysteine residues participating in disulfide bonds                                                   | 
| [Cross-link](http://www.uniprot.org/manual/crosslnk) | ft_crosslnk | Residues participating in covalent linkage(s) between proteins                                           | 

### Natural variations

| **Subsection** | **Query Field** | **Content** |
|----------------|-----------------|-------------|
| [Alternative sequence](http://www.uniprot.org/manual/var_seq) | ft_var_seq | Amino acid change(s) producing alternate protein isoforms | 
| [Natural variant](http://www.uniprot.org/manual/variant) | ft_variant | Description of a natural variant of the protein                |

### Experimental info

| **Subsection** | **Query Field** | **Content** |
|----------------|-----------------|-------------|
| [Mutagenesis](http://www.uniprot.org/manual/mutagen) | ft_mutagen | Site which has been experimentally altered by mutagenesis|
| [Sequence uncertainty](http://www.uniprot.org/manual/unsure) | ft_unsure | Regions of uncertainty in the sequence|
| [Sequence conflict](http://www.uniprot.org/manual/conflict) |ft_conflict  | Description of sequence discrepancies of unknown origin|
| [Non-adjacent residues](http://www.uniprot.org/manual/non_cons) | ft_non_cons | Indicates that two residues in a sequence are not consecutive|
| [Non-terminal residue](http://www.uniprot.org/manual/non_ter) | ft_non_ter | The sequence is incomplete. Indicate that a residue is not the terminal residue of the complete protein|

### Secondary structure

| **Subsection** | **Query Field** | **Content** |
|----------------|-----------------|-------------|
| [Helix](http://www.uniprot.org/manual/helix) | ft_helix | Helical regions within the experimentally determined protein structure |
| [Turn](http://www.uniprot.org/manual/turn) | ft_turn | Turns within the experimentally determined protein structure |
| [Beta strand](http://www.uniprot.org/manual/strand) | ft_strand | Beta strand regions within the experimentally determined protein structure |

The exact boundaries of the described sequence feature, as well as its length, are provided. When a feature is known to extend beyond the position that is given in this section, the endpoint specification will be preceded by '<' (less than) for features which continue to the N-terminal direction or by '>' (greater than) for features which continue to the C-terminal direction.

Example: [P62756](http://www.uniprot.org/uniprot/P62756#ptm%5Fprocessing)

Unknown endpoints are denoted by a question mark '?'.

Example: [P78586](http://www.uniprot.org/uniprot/P78586#ptm%5Fprocessing)

Uncertain endpoints are denoted by a question mark '?' before the position, e.g. '?42'.

Example: [Q3ZC31](http://www.uniprot.org/uniprot/Q3ZC31#ptm%5Fprocessing)

## Querying Features

Individual features can be queried using the query fields described in the tables above. Querying is of the form:

```bash
curl "http://www.ebi.ac.uk/uniprot/api/uniprotkb/search?query=FIELD:VALUE"
```

For example, to find all Human entries with variants, we could run the following `curl` command:

```bash
curl "http://www.ebi.ac.uk/uniprot/api/uniprotkb/search?query=ft_variant:* AND organism_id:9606"
```
### Feature Lengths
For any feature field, a corresponding field exists denoting its length. Given a feature, `ft_XXXX`, its length can be queried via, `ftlen_XXXX`. For example, to find Human entries with a sequence length of between 198 and 200 residues, we can execute the following command:

```
curl "http://www.ebi.ac.uk/uniprot/api/uniprotkb/search?query=ftlen_variant:[198 TO 200] AND organism_id:9606"
```

### Feature evidences
For any feature field, a corresponding field exists denoting its attached evidences (for a complete list of evidences, refer to [Evidences](evidences.md). Given a feature, `ft_XXXX`, its evidences can be queried via, `ftev_XXXX`. For example, to find Human entries with a non-traceable evidence, we can execute the following command:

```
curl "http://www.ebi.ac.uk/uniprot/api/uniprotkb/search?query=ftev_variant:ECO_0000303 AND organism_id:9606"
```


## Feature identifiers

Some features are associated with a unique and stable identifier that allows to construct links between these position-specific annotations and specialized protein-related databases.

The format of the identifiers is a 3-letter prefix, specific for an annotation type, separated by an underscore from a 6 to 10-digit number.

Feature identifiers currently exist for the following annotation topics: Propeptide, Chain, Peptide, Glycosylation, Alternative sequence and Natural variant.

| **Subsection** | **Identifier prefix** | **Availability** | **Example** | 
| **Molecule processing**

[Propeptide](http://www.uniprot.org/manual/propep)

PRO

Any processed propeptide

[Q7XAD0](http://www.uniprot.org/uniprot/Q7XAD0#ptm_processing)

[Chain](http://www.uniprot.org/manual/chain)

[Peptide](http://www.uniprot.org/manual/peptide)

PRO

Any mature polypeptide

[Q9W568](http://www.uniprot.org/uniprot/Q9W568#ptm_processing)

[P15515](http://www.uniprot.org/uniprot/P15515#ptm_processing)

**Amino acid modifications**

[Glycosylation](http://www.uniprot.org/manual/carbohyd)

CAR

Only for residues attached to an oligosaccharide structure annotated in the [GlyConnect](https://glyconnect.expasy.org/) database

[P02771](http://www.uniprot.org/uniprot/P02771#ptm_processing)

**Natural variations**

[Alternative sequence](http://www.uniprot.org/manual/var_seq)

VSP

Any sequence with an ‘Alternative sequence’ feature

[P81278](http://www.uniprot.org/uniprot/P81278#sequences)

[Natural variant](http://www.uniprot.org/manual/variant)

VAR

Only for protein sequence variants of Hominidae (great apes and humans)

[P11171](http://www.uniprot.org/uniprot/P11171#sequences)
        