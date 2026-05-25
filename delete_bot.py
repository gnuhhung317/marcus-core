#!/usr/bin/env python3
import sys
import subprocess
import json

def run_query(sql, tuples_only=True):
    cmd = ["docker", "exec", "-i", "postgres", "psql", "-U", "user", "-d", "signal_db"]
    if tuples_only:
        cmd.extend(["-t", "-A"])
    cmd.extend(["-c", sql])
    
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error running database query: {result.stderr.strip()}", file=sys.stderr)
        sys.exit(1)
    return result.stdout.strip()

def check_postgres_running():
    cmd = ["docker", "inspect", "-f", "{{.State.Running}}", "postgres"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0 or "true" not in result.stdout.lower():
        print("Error: The 'postgres' docker container is not running.", file=sys.stderr)
        print("Please start the backend services first using: docker compose up -d", file=sys.stderr)
        sys.exit(1)

def main():
    check_postgres_running()
    
    # Get Bot ID from arguments or interactive prompt
    if len(sys.argv) > 1:
        target_id = sys.argv[1]
    else:
        print("=== Marcus Trading - Bot Deletion Utility ===")
        target_id = input("Enter the Bot ID (e.g. bot_xxx) or Database UUID to delete: ").strip()
        
    if not target_id:
        print("Error: Bot ID cannot be empty.")
        sys.exit(1)
        
    # Search for the bot
    print(f"\nSearching for bot: '{target_id}'...")
    find_sql = f"""
    SELECT id, bot_id, name, trading_pair, status 
    FROM bots 
    WHERE bot_id = '{target_id}' OR id = '{target_id}';
    """
    bot_info_str = run_query(find_sql)
    
    if not bot_info_str:
        print(f"Error: No bot found with ID/UUID matching '{target_id}'.")
        sys.exit(1)
        
    # Parse bot info (Postgres values are pipe-separated with -A flag)
    parts = bot_info_str.split('|')
    internal_id = parts[0]
    bot_id = parts[1]
    bot_name = parts[2]
    trading_pair = parts[3] if len(parts) > 3 else "N/A"
    status = parts[4] if len(parts) > 4 else "UNKNOWN"
    
    print("\n" + "=" * 50)
    print("MATCHING BOT FOUND:")
    print(f"  - Database UUID : {internal_id}")
    print(f"  - Business ID   : {bot_id}")
    print(f"  - Name          : {bot_name}")
    print(f"  - Trading Pair  : {trading_pair}")
    print(f"  - Current Status: {status}")
    print("=" * 50)
    
    # Confirm deletion
    confirm = input(f"\nAre you absolutely sure you want to permanently delete this bot and ALL associated data (signals, events, subscriptions, states)? [y/N]: ").strip().lower()
    if confirm != 'y' and confirm != 'yes':
        print("Deletion canceled.")
        sys.exit(0)
        
    print("\nExecuting deletion queries...")
    
    # Build single transaction query block
    sql_transaction = f"""
    BEGIN;
    DELETE FROM execution_event WHERE signal_id IN (SELECT signal_id FROM signals WHERE bot_id = '{bot_id}');
    DELETE FROM execution_state WHERE signal_id IN (SELECT signal_id FROM signals WHERE bot_id = '{bot_id}');
    DELETE FROM signals WHERE bot_id = '{bot_id}';
    DELETE FROM bot_asset_pairs WHERE bot_entity_id = '{internal_id}';
    DELETE FROM subscriptions WHERE bot_id = '{bot_id}';
    DELETE FROM raw_events WHERE bot_id = '{bot_id}';
    DELETE FROM bots WHERE id = '{internal_id}';
    COMMIT;
    """
    
    # Run the transaction
    output = run_query(sql_transaction, tuples_only=False)
    
    # Parse line by line to show row delete counts
    lines = output.split('\n')
    table_names = [
        "Execution Events",
        "Execution States",
        "Signals",
        "Asset Pairs",
        "Subscriptions",
        "Raw Events Log",
        "Bot Instance"
    ]
    
    print("\nDeletion Complete! Details:")
    print("-" * 50)
    
    delete_index = 0
    for line in lines:
        line_clean = line.strip()
        if line_clean.startswith("DELETE"):
            try:
                count = line_clean.split()[1]
                table = table_names[delete_index]
                print(f"  - Deleted {count} records from '{table}'")
                delete_index += 1
            except IndexError:
                pass
                
    print("-" * 50)
    print("Successfully cleaned up all dependencies and removed the bot instance.")

if __name__ == "__main__":
    main()
