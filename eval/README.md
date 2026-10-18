# Running the AI Review Evaluation

The evaluation has two stages. The **sweep** runs every review approach on every model over the
corpus. The **judge** then labels the findings blind with Claude and writes the tables of the
evaluation chapter. `execute-review-eval.sh` runs both, one after the other.

| Folder | Content |
|---|---|
| `corpus/proposal/` | The four proposals `p1.pdf`–`p4.pdf`; `p4/defects.json` lists the 12 planted defects |
| `judge/` | Judge prompts, `judge.py` (pooling, checking, metrics), `tables.py` (tables), `corpus.json` (labels) |
| `results/<timestamp>/` | One folder per sweep (gitignored) |

## Prerequisites

- Java and Gradle for the server (`server/`), Python 3, and `pdfinfo` only if you check page counts.
- The `claude` CLI, logged in (`claude` once interactively), with access to `claude-opus-5-5[1m]`.
- A Logos API key in `execute-review-eval.local.sh` (gitignored). Create it once:

  ```sh
  cp execute-review-eval.local.sh.example execute-review-eval.local.sh
  # set OPENAI_API_KEY and check MODELS, SEEDS, TEMPERATURE
  ```

## 1. Smoke Test (a Few Minutes)

Run one model, one seed, every approach, and judge it. This checks the endpoint, the four
reviewers, and the `claude` judge call before the long run.

```sh
./execute-review-eval.local.sh --models=gpt-5.6-luna --seeds=1
```

Check that `eval/results/<timestamp>/judge/tables.md` exists and contains numbers. Delete the folder
afterwards so it does not mix with the real run.

## 2. Full Run (Hours)

```sh
caffeinate -i ./execute-review-eval.local.sh
```

The sweep runs 3 models × 4 approaches × 4 documents × 3 seeds = 144 reviews, then judges them.
`caffeinate` keeps the Mac awake. A failed review is recorded in `runs.jsonl` and the sweep goes on.

## 3. If the Judge Stops

Judging resumes where it stopped; answered jobs are kept:

```sh
./execute-review-judge.sh eval/results/<timestamp>
```

Lower `--parallel=4` if the CLI hits rate limits. `--fresh` starts the judging from scratch with
new random ids and batches.

## 4. Results

In `eval/results/<timestamp>/judge/`:

- `tables.md` holds every table and every value the chapter quotes.
- `tables.typ` holds the same values as Typst arrays (`eval-main-rows`, `eval-paired-rows`,
  `eval-category-rows`, `eval-filter-rows`, `eval-values`) to splice into the thesis tables.
- `results.json` holds all metrics per run and per configuration.

Tables only, for example after editing `judge/corpus.json`:

```sh
python3 -I eval/judge/judge.py aggregate eval/results/<timestamp>
python3 -I eval/judge/tables.py eval/results/<timestamp>
```
