Here are database best practices, kept simple:

**Design**
- Give tables & columns clear, obvious names
- Don't store the same data in 2 places
- Always have a primary key on every table

**Safety**
- Back up regularly, & test that restores actually work
- Don't give apps more database permissions than they need
- Never store passwords in plain text

**Speed**
- Add indexes on columns you search / filter by often
- Don't pull more data than you need (`SELECT *` is lazy & slow)
- Use connection pooling so you're not opening/closing connections constantly

**Changes**
- Never change a production database manually — use migration scripts
- Test schema changes on a dev/staging environment first

**Monitoring**
- Watch for slow queries & fix them
- Keep an eye on disk space — databases grow fast
- Set up alerts so you're not caught off guard

**General mindset**
- Automate everything you can (backups, maintenance, alerts)
- Document your schema so others (& future you) can understand it
- Simple is almost always better than clever
