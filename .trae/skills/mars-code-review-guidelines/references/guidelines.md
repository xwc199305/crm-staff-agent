# MARS Code Review Guidelines Reference

## Quick Reference Card

### PR Size Guidelines

| Size | Lines | Action |
|------|-------|--------|
| Small | < 100 | ✅ Preferred |
| Medium | 100-300 | ✅ Preferred |
| Large | 300-666 | ⚠️ Try to split |
| Extra Large | > 666 | ❌ Must split |

### Reviewer Model

| Role | Responsibility |
|------|---------------|
| Tier2 Reviewer | First review, preliminary approval |
| Tier1 Reviewer | Final approval required for merge |
| Promotion | 10+ mid/large PRs → Tier1 |

### Review Timeline

- **Max response time:** 1 business day
- **Daily slots:** 2 (morning, before EOD)
- **Max ignore time:** 2 days

---

## For Authors

### Why Small/Medium PRs?

1. **Reviewed more quickly** - Easier to find short time slots
2. **Reviewed more thoroughly** - Less fatigue, better feedback
3. **Less likely to introduce bugs** - Easier to reason about impact
4. **Less wasted work** - If rejected, less loss
5. **Easier to merge** - Fewer conflicts
6. **Easier to design well** - Polish small changes easier
7. **Less blocking** - Continue coding while waiting
8. **Easier to roll back** - Simpler dependency chain

### PR Description Template

```
<type>: <what the PR does>

<why this change is being made>
<specific implementation details>
<context and future direction>
```

### Finding Reviewers

1. Identify two reviewers familiar with codebase
2. Assign one as Tier1 (usually)
3. Avoid >3 reviewers
4. Ensure availability before assigning

---

## For Reviewers

### Review Checklist

- [ ] Code is well-designed
- [ ] Functionality is good for users
- [ ] Edge cases handled
- [ ] Concurrent programming is safe
- [ ] Code isn't overly complex
- [ ] No speculative future features
- [ ] UI changes are sensible
- [ ] Appropriate unit tests
- [ ] Tests are well-designed
- [ ] Clear names for everything
- [ ] Comments explain why, not what
- [ ] Conforms to style guides

### Comment Prefixes and Levels

**Comment Level Hierarchy (from strict to lenient):**

```
Level 1 (STRICT):   [CRITICAL] only
Level 2 (NORMAL):   [CRITICAL] + [SUGGESTION]
Level 3 (RELAXED):  [CRITICAL] + [SUGGESTION] + [NIT]
Level 4 (ALL):      [CRITICAL] + [SUGGESTION] + [NIT] + [PRAISE]
```

**Default Level:** NORMAL (Level 2)

**Categories and Level Mapping:**

| Prefix | Level | Meaning | Action Required | When to Include |
|--------|-------|---------|-----------------|-----------------|
| `[CRITICAL]` | 1 | Must fix | Yes - blocking | ALWAYS (all levels) |
| `[SUGGESTION]` | 2 | Should consider | Yes - discuss | Level 2+ (default) |
| `[NIT]` | 3 | Optional polish | No - author's choice | Level 3+ only |
| `[PRAISE]` | 4 | Good practice | N/A - encouragement | Level 4 only |

**How to detect user preference:**
- IF user says "strict review" / "only critical issues" → Level 1
- IF user says "standard review" / no preference → Level 2 (default)
- IF user says "thorough review" / "include nits" → Level 3
- IF user says "full review" / "include praise" → Level 4

**Apply Level Filter:**
```
FOR each potential comment:
  IF comment.level >= user_selected_level:
    INCLUDE in output
  ELSE:
    EXCLUDE (silently skip)
```

**Level-Based Output Examples:**

```
User: "Do a strict review of this PR"
→ Output only: [CRITICAL] comments

User: "Review this code" (default)
→ Output: [CRITICAL] + [SUGGESTION] comments

User: "Thorough review with all feedback"
→ Output: [CRITICAL] + [SUGGESTION] + [NIT] comments

User: "Full review including good practices"
→ Output: All comment types including [PRAISE]
```

### Review Principles

1. **Facts over opinions** - Technical data wins
2. **Style guide is authority** - For style matters
3. **Design has principles** - Not personal preference
4. **Consistency matters** - Match codebase if no rule applies

### Writing Good Comments

**DO:**
- Be kind
- Explain reasoning
- Balance direction with autonomy
- Encourage simplification

**DON'T:**
- Block for perfection
- Ignore code health
- Add unnecessary latency
- Make it personal

---

## External References

- [MARS Coding Conventions](https://salesforce-china.quip.com/gKz9AWm8jqii)
- [Code Review Checklist](https://salesforce-china.quip.com/ZTkZAWcIB9DD)
- [Google Engineering Practices](https://google.github.io/eng-practices/)
- [Thoughtbot Code Review Guide](https://github.com/thoughtbot/guides/tree/main/code-review)
- [Palantir Code Review Best Practices](https://blog.palantir.com/code-review-best-practices-19e02780015f)
