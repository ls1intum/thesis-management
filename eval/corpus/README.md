# Review evaluation corpus

The documents `./execute-review-eval.sh` reviews. Put PDFs here:

| Directory | Reviewed as |
|---|---|
| `eval/corpus/proposal/` | a student proposal |
| `eval/corpus/thesis/` | a final thesis |

The directory decides the review type, so a document in the wrong folder is judged against the
wrong rules. Filenames become the document ids in the results tree (`federated-learning.pdf` →
`proposal-federated-learning`), so short, descriptive names make the results easier to navigate.

## These files are committed

Everything here is part of the repository, which means it is published with it. Only put documents
in that you are entitled to publish — your own writing, or samples shared with permission. Real
student submissions do not belong here.

If you do need to evaluate against a confidential corpus, keep it outside the repository and point
the script at it:

```bash
./execute-review-eval.sh --corpus=/path/to/private/corpus --anonymize --models=...
```

`--anonymize` replaces filenames with `proposal-01`, `thesis-02` in every output path, so the
results tree carries no student identity. The id-to-filename mapping then exists only in that run's
`manifest.json`.
